/**
 * @license
 * Copyright (C) 2020 The Android Open Source Project
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
import {html} from '@polymer/polymer/lib/utils/html-tag';

export const htmlTemplate = html`
  <style include="shared-styles">
    /* Workaround for empty style block - see https://github.com/Polymer/tools/issues/408 */
  </style>
  <style include="gr-form-styles">
    tr th.emailAddressHeader,
    tr th.identityHeader {
      width: 15em;
      padding: 0 10px;
    }
    tr td.statusColumn,
    tr td.emailAddressColumn,
    tr td.identityColumn {
      word-break: break-word;
    }
    tr td.emailAddressColumn,
    tr td.identityColumn {
      padding: 4px 10px;
      width: 15em;
    }
    .deleteButton {
      float: right;
    }
    .deleteButton:not(.show) {
      display: none;
    }
    .space {
      margin-bottom: var(--spacing-l);
    }
  </style>
  <div class="gr-form-styles">
    <fieldset class="space">
      <table>
        <thead>
          <tr>
            <th class="statusHeader">Status</th>
            <th class="emailAddressHeader">Email Address</th>
            <th class="identityHeader">Identity</th>
            <th class="deleteHeader"></th>
          </tr>
        </thead>
        <tbody>
          <template
            is="dom-repeat"
            items="[[_identities]]"
            filter="filterIdentities"
          >
            <tr>
              <td class="statusColumn">[[_computeIsTrusted(item.trusted)]]</td>
              <td class="emailAddressColumn">[[item.email_address]]</td>
              <td class="identityColumn">
                [[_computeIdentity(item.identity)]]
              </td>
              <td class="deleteColumn">
                <gr-button
                  class$="deleteButton [[_computeHideDeleteClass(item.can_delete)]]"
                  on-click="_handleDeleteItem"
                >
                  Delete
                </gr-button>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </fieldset>
    <template is="dom-if" if="[[_showLinkAnotherIdentity]]">
      <fieldset>
        <a href$="[[_computeLinkAnotherIdentity()]]">
          <gr-button id="linkAnotherIdentity" link=""
            >Link Another Identity</gr-button
          >
        </a>
      </fieldset>
    </template>
  </div>
  <gr-overlay id="overlay" with-backdrop="">
    <gr-confirm-delete-item-dialog
      class="confirmDialog"
      on-confirm="_handleDeleteItemConfirm"
      on-cancel="_handleConfirmDialogCancel"
      item="[[_idName]]"
      itemTypeName="ID"
    ></gr-confirm-delete-item-dialog>
  </gr-overlay>
`;
