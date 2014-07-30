package com.rapidftr.view.fields;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.widget.GridView;
import android.widget.ImageView;
import com.rapidftr.CustomTestRunner;
import com.rapidftr.R;
import com.rapidftr.activity.BaseChildActivity;
import com.rapidftr.activity.RapidFtrActivity;
import com.rapidftr.activity.RegisterChildActivity;
import com.rapidftr.database.DatabaseHelper;
import com.rapidftr.database.ShadowSQLiteHelper;
import com.rapidftr.utils.PhotoCaptureHelper;
import com.rapidftr.utils.TestInjectionModule;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.robolectric.Robolectric;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(CustomTestRunner.class)
public class PhotoUploadBoxTest extends BaseViewSpec<PhotoUploadBox> {

    protected PhotoCaptureHelper photoCaptureHelper;
    protected Bitmap bitmap;
    protected ImageView imageView;

    @Before
    public void setUp() throws IOException {
        TestInjectionModule module = new TestInjectionModule();
        module.addBinding(DatabaseHelper.class, ShadowSQLiteHelper.getInstance());
        TestInjectionModule.setUp(this, module);
        photoCaptureHelper = mock(PhotoCaptureHelper.class);
        bitmap = mock(Bitmap.class);
        imageView = mock(ImageView.class);

        Activity activity = Robolectric.buildActivity(RegisterChildActivity.class).create().get();
        view = spy((PhotoUploadBox) activity.getLayoutInflater().inflate(R.layout.form_photo_upload_box, null));

        doReturn(imageView).when(view).getImageView();
        when(photoCaptureHelper.getCapture()).thenReturn(bitmap);

        view.photoCaptureHelper = photoCaptureHelper;
    }

    @Test
    public void shouldStartCameraActivityWhenImageClickedAndViewIsEnabled() {
        view.initialize(field, child);
        doNothing().when(view).startCapture();

        view.setEnabled(true);
        view.getImageContainer().performClick();
        verify(view).startCapture();
    }

    @Test
    public void shouldSaveCaptureWhenCapturingSuccess() throws Exception {
        view.initialize(field, child);
        RapidFtrActivity activity = (RapidFtrActivity) view.getContext();
        doNothing().when(view).saveCapture();

        activity.onActivityResult(PhotoUploadBox.CAPTURE_IMAGE_REQUEST, Activity.RESULT_OK, null);
        verify(view).saveCapture();
    }

    public void shouldCheckIfSavePhotoIsCalledWhenSaveIsSelected() throws Exception {
        view.initialize(field, child);
        RapidFtrActivity activity = (RapidFtrActivity) view.getContext();
        doCallRealMethod().when(view).saveCapture();
        doNothing().when(photoCaptureHelper).savePhoto(bitmap, 90, "");
        activity.onActivityResult(PhotoUploadBox.CAPTURE_IMAGE_REQUEST, Activity.RESULT_OK, null);
        verify(view).saveCapture();
        verify(photoCaptureHelper).savePhoto(bitmap, 90, "name");
    }

    @Test
    public void shouldNotSaveCaptureWhenCapturingCancelled() throws Exception {
        view.initialize(field, child);
        RapidFtrActivity activity = (RapidFtrActivity) view.getContext();
        activity.onActivityResult(PhotoUploadBox.CAPTURE_IMAGE_REQUEST, Activity.RESULT_CANCELED, null);
        verify(view, never()).saveCapture();
    }

    @Test
    public void shouldSetCaptureTimeWhenStartCapture() throws Exception {
        doCallRealMethod().when(photoCaptureHelper).getTempCaptureFile();
        view.startCapture();
        verify(photoCaptureHelper).setCaptureTime();
    }

    @Test
    public void shouldDeleteCapturedImagesWhenRegistrationIsCancelled() throws Exception {
        view.initialize(field, child);
        RapidFtrActivity activity = (RapidFtrActivity) view.getContext();
        doNothing().when(view).deleteCapture();

        activity.onActivityResult(BaseChildActivity.CLOSE_ACTIVITY, 999, null);
        verify(view).deleteCapture();
    }

    @Test
    public void testSaveCaptureShouldSaveBitmap() throws IOException, JSONException, GeneralSecurityException {
        view.initialize(field, child);
        doReturn(90).when(photoCaptureHelper).getPictureRotation();
        view.saveCapture();
        verify(photoCaptureHelper).savePhoto(eq(bitmap), eq(90), anyString());
    }

    @Test
    public void testSaveShouldDeleteCaptures() throws IOException, JSONException, GeneralSecurityException {
        view.initialize(field, child);
        view.saveCapture();
        verify(photoCaptureHelper).deleteCaptures();
    }

    @Test
    public void testSaveCaptureShouldSaveThumbnail() throws IOException, JSONException, GeneralSecurityException {
        view.initialize(field, child);
        doReturn(180).when(photoCaptureHelper).getPictureRotation();
        view.saveCapture();
        verify(photoCaptureHelper).saveThumbnail(eq(bitmap), eq(180), anyString());
    }

    @Test
    public void testSaveCaptureShouldSaveFileNameInChild() throws JSONException, IOException, GeneralSecurityException {
        view.initialize(field, child);
        String fileName = "random_file_name";
        doReturn(fileName).when(view).createCaptureFileName();

        view.saveCapture();
        assertThat(child.getString("current_photo_key"), equalTo("random_file_name"));
    }

    @Test
    public void shouldSaveNewlyCapturedFileNameInPhotoKeys() throws JSONException {
        view.initialize(field, child);
        String fileName = "random_file_name";
        doReturn(fileName).when(view).createCaptureFileName();

        view.saveCapture();
        assertThat(child.optJSONArray("photo_keys").length(), is(1));
        assertThat(child.optJSONArray("photo_keys").get(0).toString(), is("random_file_name"));
    }

    @Test
    public void shouldAddCapturedFileNamesToExistingPhotoKeys() throws JSONException {
        child.put("photo_keys", new JSONArray("[some_file_name]"));
        String fileName = "random_file_name";
        doReturn(fileName).when(view).createCaptureFileName();
        doNothing().when(view).setGridAttributes(Matchers.any(GridView.class), Matchers.any(JSONArray.class));
        view.initialize(field, child);

        view.saveCapture();
        assertThat(child.optJSONArray("photo_keys").length(), is(2));
        assertThat(child.optJSONArray("photo_keys").get(0).toString(), is("some_file_name"));
        assertThat(child.optJSONArray("photo_keys").get(1).toString(), is("random_file_name"));
    }

    @Test
    public void shouldSetCurrentPhotoKeyIfItIsNotSetEarlier(){
        view.initialize(field, child);
        String fileName = "some_file_name";
        doReturn(fileName).when(view).createCaptureFileName();
        view.saveCapture();
        assertThat(child.optString("current_photo_key"), is("some_file_name"));
    }

    @Test
    public void shouldSetCurrentPhotoKey(){
        view.initialize(field, child);
        Intent intent = new Intent();
        intent.putExtra("file_name","some_file");
        view.onActivityResult(PhotoUploadBox.SHOW_FULL_IMAGE_REQUEST,1, intent);
        assertEquals(child.optString("current_photo_key"),"some_file");
    }

    @Test
    public void shouldNotSetCurrentPhotoKey(){
        view.initialize(field, child);
        Intent intent = new Intent();
        view.onActivityResult(PhotoUploadBox.SHOW_FULL_IMAGE_REQUEST,1, intent);
        assertEquals(child.optString("current_photo_key"),"");
    }
}
