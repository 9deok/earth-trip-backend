package com.earthtrip.identity.adapter.out.delivery;

import org.springframework.web.util.HtmlUtils;

final class EarthTripMailBrand {

    private EarthTripMailBrand() {}

    static String header(String publicBaseUrl) {
        String logoUrl =
                HtmlUtils.htmlEscape(
                        publicBaseUrl.replaceAll("/+$", "") + "/brand/earth-trip-mark.png");
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\""
                + " style=\"margin:0 0 28px\"><tr>"
                + "<td style=\"padding-right:10px\"><img src=\""
                + logoUrl
                + "\""
                + " width=\"44\" height=\"44\" alt=\"\""
                + " style=\"display:block;width:44px;height:44px\"></td>"
                + "<td style=\"color:#3a2a27;font-family:Georgia,serif;font-size:24px;"
                + "font-weight:700;letter-spacing:-1px\">earth-trip</td>"
                + "</tr></table>";
    }
}
