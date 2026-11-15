package com.bingbang.session;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bingbang.common.ApiException;

/** Lets controllers declare a {@link Session} parameter, resolved from "Authorization: Bearer token". */
@Component
public class SessionArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PREFIX = "Bearer ";

    private final SessionService sessions;

    public SessionArgumentResolver(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == Session.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing wallet token");
        }
        return sessions.resolve(header.substring(PREFIX.length()).trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown wallet token"));
    }
}
