package com.ren.error;

/**
 * @author Ren
 */

public interface CommonError {
    public int getErrCode();

    public String getErrMsg();

    public CommonError setErrMsg(String errMsg);
}