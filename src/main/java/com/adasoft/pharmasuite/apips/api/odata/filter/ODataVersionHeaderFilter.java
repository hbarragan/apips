package com.adasoft.pharmasuite.apips.api.odata.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ODataVersionHeaderFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (req.getRequestURI() != null && req.getRequestURI().startsWith("/odata")) {
            res.setHeader("OData-Version", "4.0");
            res.setHeader("OData-MaxVersion", "4.0");
        }
        chain.doFilter(req, res);
    }
}
