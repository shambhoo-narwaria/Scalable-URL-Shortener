package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;

/**
 * =============================================================
 * URL CONTROLLER — HTTP Layer (REST API)
 * =============================================================
 *
 * WHAT IS A REST API?
 *   REST (Representational State Transfer) is a set of rules for
 *   building web services so different apps (browsers, phones, 
 *   Postman) can talk to each other over the internet.
 *   
 *   It is built on 4 core concepts that you will see in this file:
 *   
 *   1. Resources: Everything is a "resource" accessed via a URL.
 *                 (e.g., `/api/shorten` is our generator resource).
 *   
 *   2. HTTP Methods (Verbs): They define the ACTION you want to take:
 *        - POST   = Create entirely new data (Generate a short URL).
 *        - GET    = Read/Fetch existing data (Get an Analytics report or Redirect).
 *        - PUT    = Update existing data completely.
 *        - DELETE = Remove data from the database.
 *   
 *   3. JSON (JavaScript Object Notation): The universal language of REST. 
 *        We send and receive data as JSON text, making it readable by 
 *        any programming language (React, Python, Java, etc).
 *   
 *   4. Status Codes: We return standard numbers so the client knows what happened:
 *        - 200 OK: "Here is the data you asked for" (Analytics endpoint)
 *        - 201 CREATED: "I successfully created your new resource" (Shorten endpoint)
 *        - 302 FOUND: "I found your data, now go over here instead" (Redirect endpoint)
 *        - 400 BAD REQUEST: "You sent me invalid data" (Handled automatically)
 *
 * WHAT IS A CONTROLLER?
 *   The controller is the ENTRY POINT for these HTTP requests in Spring Boot.
 *   It ONLY does four things:
 *     1. Receives incoming HTTP requests (GET, POST).
 *     2. Parses request bodies and path parameters
 *     3. Calls the service layer for business logic
 *     4. Returns HTTP responses with correct status codes
 *
 * CONTROLLER RESPONSIBILITIES (and nothing more!):
 *    Parse URL path variables, query params, request body
 *    Call the appropriate service method
 *    Build the HTTP response (status code + body)
 *    NOT: business logic (that's service layer)
 *    NOT: database access (that's repository layer)
 *
 * @RestController:
 *   Combines @Controller + @ResponseBody.
 *   @Controller → registers this class to handle HTTP requests.
 *   @ResponseBody → return values from methods are automatically
 *                   serialized to JSON and written to response body.
 *
 * @RequestMapping("/"):
 *   Optional: Could set a base URL prefix here, but since we have
 *   both "/" and "/api/..." endpoints, we map individually.
 *
 * @RequiredArgsConstructor (Lombok):
 *   Generates constructor:
 *     public UrlController(UrlService urlService) {
 *         this.urlService = urlService;
 *     }
 *   Spring uses this constructor to inject the UrlService bean.
 *   This is called "Constructor Injection" — preferred over @Autowired.
 *
 *   WHY CONSTRUCTOR INJECTION OVER @Autowired?
 *     - Makes dependencies explicit
 *     - Fields can be final (immutable, thread-safe)
 *     - Easier to test (can pass mock in constructor)
 * =============================================================
 */
@CrossOrigin(origins = "*")  // Allows any frontend (like React/Vue) to call this API without CORS errors!
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Shortener Core API", description = "Endpoints for generating, redirecting, and analyzing short URLs")
public class UrlController {

    private final UrlService urlService;



    // =============================================================
    // ENDPOINT 0: GET /api (Welcome API)
    // =============================================================
    @GetMapping("/api")
    public ResponseEntity<String> welcome() {
        return ResponseEntity.ok("Welcome to the Scalable URL Shortener API! 🚀");
    }

    // =============================================================
    // ENDPOINT 1: POST /api/shorten
    // =============================================================

    /**
     * Create a short URL from a long URL.
     *
     * @PostMapping:
     *   Maps HTTP POST requests to /api/shorten to this method.
     *
     * @RequestBody:
     *   Tells Spring to deserialize the JSON request body into
     *   a ShortenRequest Java object.
     *
     *   Incoming JSON:
     *   {
     *     "url": "https://example.com/very/long/path",
     *     "customCode": "optional",
     *     "expiryDays": 30
     *   }
     *
     *   Jackson (Spring's JSON library) converts this JSON → ShortenRequest object.
     *
     * @Valid:
     *   Triggers Bean Validation on the ShortenRequest object.
     *   Checks all @NotBlank, @Size, @Pattern annotations.
     *   If any fail → automatically throws MethodArgumentNotValidException
     *               → GlobalExceptionHandler catches it → HTTP 400 returned
     *
     * ResponseEntity<ShortenResponse>:
     *   Gives you control over the full HTTP response:
     *     - Status code (201 Created, not just 200)
     *     - Response body (ShortenResponse object → serialized to JSON)
     *     - Response headers (if needed)
     *
     * HTTP 201 Created:
     *   The correct status for "resource was created".
     *   HTTP 200 is for "request succeeded and here's data".
     *   200 vs 201 distinction shows API design knowledge to interviewers.
     *
     * @param request  Validated ShortenRequest from JSON body
     * @return         201 Created with ShortenResponse JSON body
     */
    @Operation(summary = "Create a short URL", description = "Takes a long URL and generates a Base62 short code.")
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @Valid @RequestBody ShortenRequest request) {

        log.info("POST /api/shorten | URL: {}", request.getUrl());

        ShortenResponse response = urlService.shortenUrl(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)  // 201 Created (not 200 OK)
            .body(response);
    }

    // =============================================================
    // ENDPOINT 2: GET /{shortCode}  ← THE REDIRECT
    // =============================================================

    /**
     * Redirect a short code to its original long URL.
     *
     * This is the MOST CALLED endpoint — every click on a short URL
     * hits this method.
     *
     * @GetMapping("/{shortCode}"):
     *   Maps GET /{anything} to this method.
     *   The {shortCode} is a path variable.
     *
     * @PathVariable:
     *   Extracts the {shortCode} from the URL path.
     *   GET /aB12x → shortCode = "aB12x"
     *   GET /xyz99 → shortCode = "xyz99"
     *
     * HOW HTTP REDIRECT WORKS:
     *   Client sends: GET /aB12x
     *   Server responds:
     *     HTTP 302 Found
     *     Location: https://example.com/very/long/url
     *
     *   Browser sees 302 + Location header → automatically
     *   makes a NEW GET request to the Location URL.
     *   User ends up at the original URL (transparent to them).
     *
     * 301 vs 302?
     *   301 = Permanent Redirect (browser CACHES this forever)
     *         → If you later change the URL, browser ignores the new destination
     *   302 = Temporary Redirect (browser always asks the server)
     *         → Click count always registered
     *         → Can change destination anytime
     *   → We use 302 for URL shorteners. ALWAYS. 301 breaks analytics.
     *
     * @param shortCode  The short code from the URL path
     * @return           302 Redirect with Location header set to long URL
     */
    @Operation(summary = "Redirect to Long URL", description = "Simulates a user clicking the short link. Handles the HTTP 302 Redirect.")
    @GetMapping("/{shortCode:[a-zA-Z0-9_\\-]+}")
    public ResponseEntity<Void> redirect(@PathVariable("shortCode") String shortCode) {
        log.info("GET /{} — redirect requested", shortCode);

        // Resolve the short code → long URL
        // This handles cache-aside, DB lookup, expiry check
        String longUrl = urlService.resolveUrl(shortCode);

        // Build the redirect response
        // ResponseEntity<Void> means "no response body" — just headers
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(longUrl));  // Sets the Location header

        // Return 302 Found with Location header
        return new ResponseEntity<>(headers, HttpStatus.FOUND);

        // NOTE: HttpStatus.FOUND = 302
        // If you want permanent redirect: HttpStatus.MOVED_PERMANENTLY = 301
    }

    // =============================================================
    // ENDPOINT 3: GET /api/analytics/{shortCode}
    // =============================================================

    /**
     * Get click analytics for a specific short URL.
     *
     * @param shortCode  The short code to get analytics for
     * @return           200 OK with AnalyticsResponse JSON body
     */
    @Operation(summary = "Get Click Analytics", description = "Returns the total number of clicks and metadata for a specific short code.")
    @GetMapping("/api/analytics/{shortCode}")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @PathVariable("shortCode") String shortCode) {

        log.info("GET /api/analytics/{}", shortCode);

        AnalyticsResponse analytics = urlService.getAnalytics(shortCode);

        return ResponseEntity.ok(analytics); // 200 OK with body
    }
}
