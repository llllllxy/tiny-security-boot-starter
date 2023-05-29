package org.bluewind.authclient.provider;

import org.bluewind.authclient.AuthProperties;
import org.bluewind.authclient.util.CommonUtil;
import org.bluewind.authclient.util.TokenGenUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 操作token和会话的接口（通过jdbc实现）
 *
 * @author liuxingyu01
 * @version 2023-01-06-9:33
 **/
public class JdbcAuthProvider extends AbstractAuthProvider implements AuthProvider {
    final static Logger log = LoggerFactory.getLogger(JdbcAuthProvider.class);

    private final JdbcTemplate jdbcTemplate;

    private final AuthProperties authProperties;

    public JdbcAuthProvider(JdbcTemplate jdbcTemplate, AuthProperties authProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.authProperties = authProperties;
    }

    @Override
    protected AuthProperties getAuthProperties() {
        return this.authProperties;
    }


    /**
     * 刷新token
     *
     * @param token
     * @return
     */
    @Override
    public boolean refreshToken(String token) {
        try {
            String sql = "update " + this.getAuthProperties().getTableName() + " set token_expire_time = ? where token_str = ?";
            int num = jdbcTemplate.update(sql, CommonUtil.currentTimePlusSeconds(this.getAuthProperties().getTimeout()), token);
            return num > 0;
        } catch (Exception e) {
            log.error("JdbcAuthProvider - refreshToken - 失败，Exception：{e}", e);
            return false;
        }
    }

    /**
     * 检查token是否失效
     *
     * @param token
     * @return
     */
    @Override
    public boolean checkToken(String token) {
        try {
            String sql = "select token_str,login_id,token_expire_time from " + this.getAuthProperties().getTableName() + " where token_str=?";
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql, token);
            if (Objects.nonNull(resultList) && !resultList.isEmpty()) {
                String tokenExpireTime = resultList.get(0).get("token_expire_time").toString();
                return CommonUtil.timeCompare(tokenExpireTime);
            }
            return false;
        } catch (Exception e) {
            log.error("JdbcAuthProvider - checkToken - 失败，Exception：{e}", e);
            return false;
        }
    }

    /**
     * 创建一个新的token
     *
     * @param loginId 会话登录：参数填写要登录的账号id，建议的数据类型：long | int | String， 不可以传入复杂类型，如：User、Admin 等等
     * @return
     */
    @Override
    public String createToken(Object loginId) {
        try {
            String token = TokenGenUtil.genTokenStr(this.getAuthProperties().getTokenStyle());
            String sql = "insert into " + this.getAuthProperties().getTableName() + " (token_str,login_id,token_expire_time) values (?,?,?)";
            int num = jdbcTemplate.update(sql, token, String.valueOf(loginId), CommonUtil.currentTimePlusSeconds(this.getAuthProperties().getTimeout()));
            return num > 0 ? token : null;
        } catch (Exception e) {
            log.error("JdbcAuthProvider - createToken - 失败，Exception：{e}", e);
            return null;
        }
    }

    /**
     * 根据token，获取loginId
     *
     * @param token
     * @return
     */
    @Override
    public Object getLoginId(String token) {
        try {
            String sql = "select token_str,login_id,token_expire_time from " + this.getAuthProperties().getTableName() + " where token_str=?";
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql, token);
            if (Objects.nonNull(resultList) && !resultList.isEmpty()) {
                return resultList.get(0).get("login_id");
            }
            return null;
        } catch (Exception e) {
            log.error("JdbcAuthProvider - getLoginId - 失败，Exception：{e}", e);
            return null;
        }
    }

    /**
     * 删除token
     *
     * @param token
     * @return
     */
    @Override
    public boolean deleteToken(String token) {
        try {
            String sql = "delete from " + this.getAuthProperties().getTableName() + " where token_str = ?";
            int num = jdbcTemplate.update(sql, token);
            return num > 0;
        } catch (Exception e) {
            log.error("JdbcAuthProvider - deleteToken - 失败，Exception：{e}", e);
            return false;
        }
    }

    /**
     * 通过loginId删除token
     *
     * @param loginId
     * @return
     */
    @Override
    public boolean deleteTokenByLoginId(Object loginId) {
        try {
            String sql = "delete from " + this.getAuthProperties().getTableName() + " where login_id = ?";
            int num = jdbcTemplate.update(sql, loginId);
            return num > 0;
        } catch (Exception e) {
            log.error("JdbcAuthProvider - deleteTokenByLoginId - 失败，Exception：{e}", e);
            return false;
        }
    }

}
