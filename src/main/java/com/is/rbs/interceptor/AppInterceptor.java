package com.is.rbs.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AppInterceptor implements HandlerInterceptor {

   // @Value("${TOKEN_URL}")
   // private String tokenURL;

    
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

//        String token = request.getHeader("Authorization");
//        if (token == null) {
//            response.getWriter().write(Utils.objectToString(new Response(Status.ERROR, "Token_not_found", null)));
//            return false;
//        }
        return true;
    }

    
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
