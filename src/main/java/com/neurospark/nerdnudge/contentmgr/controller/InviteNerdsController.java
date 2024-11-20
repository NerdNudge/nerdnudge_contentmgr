package com.neurospark.nerdnudge.contentmgr.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nerdnudge/invitenerds")
public class InviteNerdsController {
    private static final String ANDROID_APP_LINK = "https://play.google.com/store/apps/details?id=com.neurospark.nerdnudge";
    private static final String IOS_APP_LINK = "https://play.google.com/store/apps/details?id=com.neurospark.nerdnudge";
    private static final String FALLBACK_URL = "https://nerdnudge.com";

    @GetMapping("/invite")
    public ResponseEntity<Void> redirectToStore(@RequestHeader(value = "User-Agent", defaultValue = "") String userAgent) {
        String redirectUrl;

        if (userAgent.toLowerCase().contains("android")) {
            redirectUrl = ANDROID_APP_LINK;
        } else if (userAgent.toLowerCase().contains("iphone") || userAgent.toLowerCase().contains("ipad")) {
            redirectUrl = IOS_APP_LINK;
        } else {
            redirectUrl = FALLBACK_URL;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUrl);
        System.out.println("Redirecting to: " + redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
