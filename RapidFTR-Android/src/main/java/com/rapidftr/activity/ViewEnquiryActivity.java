package com.rapidftr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.google.inject.Inject;
import com.rapidftr.R;
import com.rapidftr.RapidFtrApplication;
import com.rapidftr.service.EnquiryHttpDao;
import com.rapidftr.service.EnquirySyncService;
import com.rapidftr.service.LogOutService;
import com.rapidftr.task.AsyncTaskWithDialog;
import com.rapidftr.task.SyncSingleRecordTask;
import org.json.JSONException;

import java.io.IOException;

public class ViewEnquiryActivity extends BaseEnquiryActivity {
    @Inject
    private LogOutService logOutService;

    @Override
    protected void initializeView() {
        setContentView(R.layout.activity_view_enquiry);
    }

    @Override
    protected void initializeData(Bundle savedInstanceState) throws JSONException, IOException {
        super.initializeData(savedInstanceState);
        this.editable = false;
        this.enquiry = loadEnquiry(getIntent().getExtras(), enquiryRepository);
    }

    @Override
    protected void initializeLabels() throws JSONException {
        setLabel(R.string.edit);
        setTitle(enquiry.getShortId());
    }

    public void edit(View view) throws JSONException {
        Intent editEnquiryIntent = new Intent(this, EditEnquiryActivity.class);
        editEnquiryIntent.putExtra("id", enquiry.getUniqueId());
        startActivity(editEnquiryIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sync_single_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_password:
                startActivity(new Intent(this, ChangePasswordActivity.class));
                return true;
            case R.id.sync_single:
                sync();
                return true;
            case R.id.logout:
                logOutService.attemptLogOut(this);
                return true;
            case R.id.info:
                startActivity(new Intent(this, InfoActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void sync() {
        SyncSingleRecordTask syncRecordTask = createSyncTaskForEnquiry();
        syncRecordTask.setActivity(this);
        RapidFtrApplication.getApplicationInstance().setAsyncTaskWithDialog((AsyncTaskWithDialog) AsyncTaskWithDialog.wrap(this, syncRecordTask, R.string.sync_progress, R.string.sync_success, R.string.sync_failure).execute(enquiry));
    }

    protected SyncSingleRecordTask createSyncTaskForEnquiry() {
        SyncSingleRecordTask syncRecordTask = new SyncSingleRecordTask(
                new EnquirySyncService(this.getContext().getSharedPreferences(), new EnquiryHttpDao(), enquiryRepository), enquiryRepository, getCurrentUser());
        return syncRecordTask;
    }
}
