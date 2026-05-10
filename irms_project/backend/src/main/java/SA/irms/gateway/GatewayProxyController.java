package SA.irms.gateway;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import SA.irms.common.error.DomainException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
class GatewayProxyController {
    private static final List<String> HOP_BY_HOP_HEADERS = List.of(
            HttpHeaders.CONNECTION,
            HttpHeaders.TRANSFER_ENCODING,
            HttpHeaders.UPGRADE,
            HttpHeaders.PROXY_AUTHENTICATE,
            HttpHeaders.PROXY_AUTHORIZATION
    );

    private final RestTemplate restTemplate;
    private final GatewayRouteLocator routeLocator;

    GatewayProxyController(@Qualifier("gatewayRestTemplate") RestTemplate restTemplate, GatewayRouteLocator routeLocator) {
        this.restTemplate = restTemplate;
        this.routeLocator = routeLocator;
    }

    @RequestMapping("/api/**")
    ResponseEntity<byte[]> proxy(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI();
        URI targetUri = UriComponentsBuilder
                .fromHttpUrl(routeLocator.targetBaseUrl(path, request.getMethod()))
                .path(path)
                .query(request.getQueryString())
                .build(true)
                .toUri();

        RequestEntity<byte[]> requestEntity = new RequestEntity<>(
                body == null ? new byte[0] : body,
                copyHeaders(request),
                HttpMethod.valueOf(request.getMethod()),
                targetUri
        );

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(requestEntity, byte[].class);
            return new ResponseEntity<>(response.getBody(), filterResponseHeaders(response.getHeaders()), response.getStatusCode());
        } catch (RestClientResponseException exception) {
            HttpHeaders responseHeaders = exception.getResponseHeaders() == null ? new HttpHeaders() : exception.getResponseHeaders();
            return new ResponseEntity<>(
                    exception.getResponseBodyAsByteArray(),
                    filterResponseHeaders(responseHeaders),
                    exception.getStatusCode()
            );
        } catch (ResourceAccessException exception) {
            throw new DomainException(
                    HttpStatus.BAD_GATEWAY,
                    "bad_gateway",
                    "The upstream service could not be reached."
            );
        }
    }

    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames()).forEach(name -> {
            if (!isHopByHop(name)) {
                headers.put(name, Collections.list(request.getHeaders(name)));
            }
        });
        return headers;
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders source) {
        HttpHeaders headers = new HttpHeaders();
        source.forEach((name, values) -> {
            if (!isHopByHop(name)) {
                headers.put(name, values);
            }
        });
        return headers;
    }

    private boolean isHopByHop(String headerName) {
        return HOP_BY_HOP_HEADERS.stream().anyMatch(header -> header.equalsIgnoreCase(headerName));
    }
}
