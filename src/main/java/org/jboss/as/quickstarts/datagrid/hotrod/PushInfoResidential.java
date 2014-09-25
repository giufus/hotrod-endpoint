package org.jboss.as.quickstarts.datagrid.hotrod;

import java.io.Serializable;

/**
 * Created by giufus on 24/09/14.
 */
public class PushInfoResidential implements Serializable{

    private static final long serialVersionUID = -181403229462003401L;

    private String cf;
    private String pushToken;

    public String getCf() {
        return cf;
    }

    public void setCf(String cf) {
        this.cf = cf;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public PushInfoResidential(String cf, String pushToken) {
        this.cf = cf;
        this.pushToken = pushToken;
    }

    @Override
    public String toString() {
        return "PushInfoResidential{" +
                "cf='" + cf + '\'' +
                ", pushToken='" + pushToken + '\'' +
                '}' + '\n';
    }
}
