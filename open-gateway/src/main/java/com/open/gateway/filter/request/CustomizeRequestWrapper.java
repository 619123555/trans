package com.open.gateway.filter.request;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.open.common.constants.CommonEnum;
import com.open.common.exception.GatewayException;
import com.open.common.utils.RsaUtils;
import com.open.gateway.entity.GatewayRsa;
import com.open.gateway.service.impl.RsaInfoServiceImpl;
import com.open.gateway.util.WebConstant;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomizeRequestWrapper extends HttpServletRequestWrapper {

    private static final Logger logger = LoggerFactory.getLogger(CustomizeRequestWrapper.class);
    private String body;
    private String securityKey;
    private String organizationId;
    private String service;
    private String msg;

    public CustomizeRequestWrapper(HttpServletRequest httpServletRequest, RsaInfoServiceImpl rsaInfoService)
            throws GatewayException {
        super(httpServletRequest);
        try {
            this.decryptRequestBody(rsaInfoService);
        } catch (Throwable e) {
            logger.error("Exception", e);
            this.msg = CommonEnum.ISV_INVALID_ENCRYPT.getMsg();
        }
    }

    private void decryptRequestBody(RsaInfoServiceImpl rsaInfoService) throws IOException, GatewayException {
        InputStream is = null;
        try {
            is = super.getInputStream();
            JSONObject jsonObject = JSON.parseObject(IoUtil.read(is, WebConstant.DEFAULT_CHARSET));
            String data = jsonObject.getString(WebConstant.ENCRYPTED_KEY);
            String organizationId = jsonObject.getString(WebConstant.ORGANIZATION_ID);
            GatewayRsa pub = rsaInfoService.selectGatewayRsa(organizationId);
            if (pub == null || pub.getRsaStatus() != 0) {
                this.msg = CommonEnum.ISV_AUTH_RSA_TIME_OUT.getMsg();
                return;
            }
            this.initData(pub.getChannelPublicKey(), organizationId, jsonObject.getString(WebConstant.SERVICE_KEY));
            jsonObject.put(WebConstant.SYSTEM_ID, pub.getSystemId());
            jsonObject.put(WebConstant.ORGANIZATION_ID, pub.getOrganizationId());
            jsonObject.put(WebConstant.ENCRYPTED_KEY, new String(
                RsaUtils.decryptByPrivateKey(Base64.decode(data), pub.getPrivateKey())));
            this.body = jsonObject.toJSONString();
        } catch (IOException e) {
            logger.error("IOException:", e);
            throw new GatewayException(CommonEnum.ISV_INVALID_ENCRYPT.getMsg());
        } catch (Exception e) {
            logger.error("Exception:", e);
            throw new GatewayException(CommonEnum.ISV_INVALID_ENCRYPT.getMsg());
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CustomizeServletInputStream(new ByteArrayInputStream(body.getBytes()));
    }

    @Override
    public BufferedReader getReader() throws IOException {
        String enc = getCharacterEncoding() == null ? WebConstant.DEFAULT_CHARSET : getCharacterEncoding();
        return new BufferedReader(new InputStreamReader(getInputStream(), enc));
    }

    private void initData(String securityKey, String organizationId, String service) {
        this.securityKey = securityKey;
        this.organizationId = organizationId;
        this.service = service;
    }

    public String getSecurityKey() {
        return this.securityKey;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getService() {
        return service;
    }

    public String getMsg() {
        return msg;
    }
}
