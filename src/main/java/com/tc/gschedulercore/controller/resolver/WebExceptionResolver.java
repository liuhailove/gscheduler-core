package com.tc.gschedulercore.controller.resolver;

import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.exception.JobException;
import com.tc.gschedulercore.core.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * common exception resolver
 *
 * @author honggang.liu 2016-1-6 19:22:18
 */
@Component
public class WebExceptionResolver implements HandlerExceptionResolver {
	private static  Logger logger = LoggerFactory.getLogger(WebExceptionResolver.class.getSimpleName());

	@Override
	public ModelAndView resolveException(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex) {

		if (!(ex instanceof JobException)) {
			logger.error("WebExceptionResolver:{}", ex);
		}

		// if json
		boolean isJson = false;
		if (handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			ResponseBody responseBody = method.getMethodAnnotation(ResponseBody.class);
			if (responseBody != null) {
				isJson = true;
			}
		}

		// error result
		ReturnT<String> errorResult = new ReturnT<String>(ReturnT.FAIL_CODE, ex.toString().replaceAll("\n", "<br/>"));

		// response
		ModelAndView mv = new ModelAndView();
		if (isJson) {
			try {
				response.setContentType("application/json;charset=utf-8");
				response.getWriter().print(JacksonUtil.writeValueAsString(errorResult));
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			mv.addObject("exceptionMsg", errorResult.getMsg());
			mv.setViewName("/common/common.exception");
		}
		return mv;
	}
	
}