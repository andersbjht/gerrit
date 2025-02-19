// Copyright (C) 2010 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.httpd.auth.container;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSession;
import com.google.gerrit.server.account.AccountException;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.account.AuthRequest;
import com.google.gerrit.server.account.AuthResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Singleton
class HttpsClientSslCertAuthFilter implements Filter {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final Pattern REGEX_USERID = Pattern.compile("CN=([^,]*)");

  private final DynamicItem<WebSession> webSession;
  private final AccountManager accountManager;
  private final AuthRequest.Factory authRequestFactory;

  @Inject
  HttpsClientSslCertAuthFilter(
      final DynamicItem<WebSession> webSession,
      AccountManager accountManager,
      final AuthRequest.Factory authRequestFactory) {
    this.webSession = webSession;
    this.accountManager = accountManager;
    this.authRequestFactory = authRequestFactory;
  }

  @Override
  public void destroy() {}

  @Override
  public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain chain)
      throws IOException, ServletException {
    X509Certificate[] certs =
        (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
    if (certs == null || certs.length == 0) {
      throw new ServletException(
          "Couldn't get the attribute javax.servlet.request.X509Certificate from the request");
    }
    String name = certs[0].getSubjectDN().getName();
    Matcher m = REGEX_USERID.matcher(name);
    String userName;
    if (m.find()) {
      userName = m.group(1);
    } else {
      throw new ServletException("Couldn't extract username from your certificate");
    }
    final AuthRequest areq = authRequestFactory.createForUser(userName);
    final AuthResult arsp;
    try {
      arsp = accountManager.authenticate(areq);
    } catch (AccountException e) {
      String err = "Unable to authenticate user \"" + userName + "\"";
      logger.atSevere().withCause(e).log(err);
      throw new ServletException(err, e);
    }
    webSession.get().login(arsp, true);
    chain.doFilter(req, rsp);
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {}
}
