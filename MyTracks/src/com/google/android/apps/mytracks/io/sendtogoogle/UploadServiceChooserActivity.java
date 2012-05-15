/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.apps.mytracks.io.sendtogoogle;

import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TableRow;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A chooser to select the Google services to upload a track to.
 * 
 * @author Jimmy Shih
 */
public class UploadServiceChooserActivity extends Activity {

  private static final int DIALOG_CHOOSER_ID = 0;

  private SendRequest sendRequest;
  private AlertDialog alertDialog;

  private CheckBox mapsCheckBox;
  private CheckBox fusionTablesCheckBox;
  private CheckBox docsCheckBox;

  private TableRow mapsOptionTableRow;
  private RadioButton newMapRadioButton;
  private RadioButton existingMapRadioButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
  }

  @Override
  protected void onResume() {
    super.onResume();
    showDialog(DIALOG_CHOOSER_ID);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_CHOOSER_ID) {
      return null;
    }
    View view = getLayoutInflater().inflate(R.layout.upload_service_chooser, null);

    mapsCheckBox = (CheckBox) view.findViewById(R.id.send_google_maps);
    fusionTablesCheckBox = (CheckBox) view.findViewById(R.id.send_google_fusion_tables);
    docsCheckBox = (CheckBox) view.findViewById(R.id.send_google_docs);

    mapsOptionTableRow = (TableRow) view.findViewById(R.id.send_google_maps_option_row);
    newMapRadioButton = (RadioButton) view.findViewById(R.id.send_google_new_map);
    existingMapRadioButton = (RadioButton) view.findViewById(R.id.send_google_existing_map);

    // Setup checkboxes
    OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton button, boolean checked) {
        updateStateBySelection();
      }
    };
    mapsCheckBox.setOnCheckedChangeListener(checkBoxListener);
    fusionTablesCheckBox.setOnCheckedChangeListener(checkBoxListener);
    docsCheckBox.setOnCheckedChangeListener(checkBoxListener);

    // Setup initial state
    initState();

    // Update state based on current selection
    updateStateBySelection();

    alertDialog = new AlertDialog.Builder(this)
        .setCancelable(true)
        .setNegativeButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface d) {
            finish();
          }
        })
        .setPositiveButton(R.string.send_google_send_now, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            saveState();
            if (sendMaps() || sendFusionTables() || sendDocs()) {
              startNextActivity();
            } else {
              Toast.makeText(UploadServiceChooserActivity.this,
                  R.string.send_google_no_service_selected, Toast.LENGTH_LONG).show();
              finish();
            }
          }
        })
        .setTitle(R.string.send_google_title)
        .setView(view)
        .create();
    return alertDialog;
  }

  /**
   * Initializes the UI state based on the shared preferences.
   */
  @VisibleForTesting
  void initState() {
    boolean pickExistingMap = PreferencesUtils.getBoolean(
        this, R.string.pick_existing_map_key, PreferencesUtils.PICK_EXISTING_MAP_DEFAULT);

    newMapRadioButton.setChecked(!pickExistingMap);
    existingMapRadioButton.setChecked(pickExistingMap);

    mapsCheckBox.setChecked(PreferencesUtils.getBoolean(
        this, R.string.send_to_maps_key, PreferencesUtils.SEND_TO_MAPS_DEFAULT));
    fusionTablesCheckBox.setChecked(PreferencesUtils.getBoolean(
        this, R.string.send_to_fusion_tables_key, PreferencesUtils.SEND_TO_FUSION_TABLES_DEFAULT));
    docsCheckBox.setChecked(PreferencesUtils.getBoolean(
        this, R.string.send_to_docs_key, PreferencesUtils.SEND_TO_DOCS_DEFAULT));
  }

  /**
   * Updates the UI state based on the current selection.
   */
  private void updateStateBySelection() {
    mapsOptionTableRow.setVisibility(sendMaps() ? View.VISIBLE : View.GONE);
  }

  /**
   * Saves the UI state to the shared preferences.
   */
  @VisibleForTesting
  void saveState() {
    PreferencesUtils.setBoolean(
        this, R.string.pick_existing_map_key, existingMapRadioButton.isChecked());
    PreferencesUtils.setBoolean(this, R.string.send_to_maps_key, sendMaps());
    PreferencesUtils.setBoolean(this, R.string.send_to_fusion_tables_key, sendFusionTables());
    PreferencesUtils.setBoolean(this, R.string.send_to_docs_key, sendDocs());
  }

  /**
   * Returns true to send to Google Maps.
   */
  private boolean sendMaps() {
    return mapsCheckBox.isChecked();
  }

  /**
   * Returns true to send to Google Fusion Tables.
   */
  private boolean sendFusionTables() {
    return fusionTablesCheckBox.isChecked();
  }

  /**
   * Returns true to send to Google Docs.
   */
  private boolean sendDocs() {
    return docsCheckBox.isChecked();
  }

  /**
   * Starts the next activity, {@link AccountChooserActivity}.
   */
  @VisibleForTesting
  protected void startNextActivity() {
    sendStats();
    sendRequest.setSendMaps(sendMaps());
    sendRequest.setSendFusionTables(sendFusionTables());
    sendRequest.setSendDocs(sendDocs());
    sendRequest.setNewMap(!existingMapRadioButton.isChecked());
    Intent intent = IntentUtils.newIntent(this, AccountChooserActivity.class)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
    finish();
  }

  /**
   * Sends stats to Google Analytics.
   */
  private void sendStats() {
    ArrayList<String> pages = new ArrayList<String>();
    if (sendRequest.isSendMaps()) {
      pages.add("/send/maps");
    }
    if (sendRequest.isSendFusionTables()) {
      pages.add("/send/fusion_tables");
    }
    if (sendRequest.isSendDocs()) {
      pages.add("/send/docs");
    }
    AnalyticsUtils.sendPageViews(this, pages.toArray(new String[pages.size()]));
  }

  @VisibleForTesting
  AlertDialog getAlertDialog() {
    return alertDialog;
  }

  @VisibleForTesting
  SendRequest getSendRequest() {
    return sendRequest;
  }
}
