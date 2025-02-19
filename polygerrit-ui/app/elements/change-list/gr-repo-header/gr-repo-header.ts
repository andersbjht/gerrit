/**
 * @license
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {GerritNav} from '../../core/gr-navigation/gr-navigation';
import {RepoName} from '../../../types/common';
import {WebLinkInfo} from '../../../types/diff';
import {appContext} from '../../../services/app-context';
import {sharedStyles} from '../../../styles/shared-styles';
import {fontStyles} from '../../../styles/gr-font-styles';
import {dashboardHeaderStyles} from '../../../styles/dashboard-header-styles';
import {LitElement, css, html, PropertyValues} from 'lit';
import {customElement, property} from 'lit/decorators';

@customElement('gr-repo-header')
export class GrRepoHeader extends LitElement {
  @property({type: String})
  repo?: RepoName;

  @property({type: String})
  _repoUrl: string | null = null;

  @property({type: Array})
  _webLinks: WebLinkInfo[] = [];

  private readonly restApiService = appContext.restApiService;

  static override get styles() {
    return [
      sharedStyles,
      dashboardHeaderStyles,
      fontStyles,
      css`
        .browse {
          display: inline-block;
          font-weight: var(--font-weight-bold);
          text-align: right;
          width: 4em;
        }
        a {
          padding-right: 0.3em;
        }
      `,
    ];
  }

  _renderLinks(webLinks: WebLinkInfo[]) {
    if (!webLinks) return;
    return html`<div>
      <span class="browse">Browse:</span>
      ${webLinks.map(
        link => html`<a target="_blank" href="${link.url}">${link.name}</a> `
      )}
    </div> `;
  }

  override render() {
    return html` <div class="info">
      <h1 class="heading-1">${this.repo}</h1>
      <hr />
      <div>
        <span>Detail:</span> <a href="${this._repoUrl!}">Repo settings</a>
      </div>
      ${this._renderLinks(this._webLinks)}
    </div>`;
  }

  override updated(changedProperties: PropertyValues) {
    if (changedProperties.has('repo')) {
      this._repoChanged();
    }
  }

  _repoChanged() {
    const repoName = this.repo;
    if (!repoName) {
      this._repoUrl = null;
      return;
    }

    this._repoUrl = GerritNav.getUrlForRepo(repoName);

    this.restApiService.getRepo(repoName).then(repo => {
      if (!repo?.web_links) return;
      this._webLinks = repo.web_links;
    });
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'gr-repo-header': GrRepoHeader;
  }
}
