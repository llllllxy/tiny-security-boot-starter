package org.tinycloud.security.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;
import org.tinycloud.security.config.GlobalConfigUtils;
import org.tinycloud.security.exception.UnAuthorizedException;
import org.tinycloud.security.interceptor.holder.AuthenticeHolder;
import org.tinycloud.security.provider.AuthProvider;
import org.tinycloud.security.provider.LoginSubject;
import org.tinycloud.security.util.AuthUtil;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 用户会话验证拦截器
 *
 * @author liuxingyu01
 * @version 2020-03-22-11:23
 **/
public class AuthenticeInterceptor implements HandlerInterceptor {

    /**
     * 存储会话的接口
     */
    private AuthProvider authProvider;

    public AuthProvider getAuthProvider() {
        return this.authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }


    public AuthenticeInterceptor(AuthProvider authProvider) {
        this.setAuthProvider(authProvider);
    }


    /*
     * 进入controller层之前拦截请求
     * 返回值：表示是否将当前的请求拦截下来  false：拦截请求，请求别终止。true：请求不被拦截，继续执行
     * Object obj:表示被拦的请求的目标对象（controller中方法）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 判断请求类型，如果是OPTIONS，直接返回
        String options = HttpMethod.OPTIONS.toString();
        if (options.equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        // 检查是否忽略会话验证
        Method method = ((HandlerMethod) handler).getMethod();
        if (AuthUtil.checkIgnore(method)) {
            return true;
        }

        // 第一步、先从请求的request里获取传来的token值，并且判断token值是否为空
        String token = this.getAuthProvider().getToken(request);
        if (StringUtils.isEmpty(token)) {
            // 直接抛出异常的话，就不需要return false了
            throw new UnAuthorizedException();
        }

        // 第二步、再判断此token值在会话存储器中是否存在，存在的话说明会话有效，并刷新会话时长
        LoginSubject subject = this.getAuthProvider().getSubject(token);
        if (Objects.isNull(subject)) {
            throw new UnAuthorizedException();
        } else {
            long expireTime = subject.getLoginExpireTime();
            long currentTime = System.currentTimeMillis();
            int timeout = GlobalConfigUtils.getGlobalConfig().getTimeout();
            long millsCritical = (long) Math.floor(timeout * 1000L * 0.6);
            if (expireTime - currentTime <= millsCritical) {
                // 刷新会话缓存时长
                subject.setLoginExpireTime(currentTime + timeout * 1000L);
                boolean result = this.getAuthProvider().refreshToken(token, subject);
            }
            // 存入LoginId，以方便后续使用
            AuthenticeHolder.setLoginId(subject.getLoginId());
            // 合格不需要拦截，放行
            return true;
        }
    }


    /*
     * 处理请求完成后视图渲染之前的处理操作
     * 通过ModelAndView参数改变显示的视图，或发往视图的方法
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
    }

    /*
     * 视图渲染之后的操作
     */
    @Override
    public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3) throws Exception {
        AuthenticeHolder.clearLoginId();
    }
}
