/*
 *  Copyright (c) 2015 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 *  following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial
 *  portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  @author Dennis Lang  (Dec-2015)
 *  @see <a href="https://landenlabs.com">https://landenlabs.com</a>
 *
 */

package com.landenlabs.all_encrypnotes;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.landenlabs.all_encrypnotes.code.EncrypPrefs;
import com.landenlabs.all_encrypnotes.code.SendMsg;
import com.landenlabs.all_encrypnotes.code.Util;
import com.landenlabs.all_encrypnotes.code.doc.Doc;
import com.landenlabs.all_encrypnotes.code.doc.DocFileDlg;
import com.landenlabs.all_encrypnotes.code.doc.FileListAdapter;
import com.landenlabs.all_encrypnotes.code.pwd.UiPasswordManager;
import com.landenlabs.all_encrypnotes.ui.DlgClickListener;
import com.landenlabs.all_encrypnotes.ui.Email;
import com.landenlabs.all_encrypnotes.ui.HomeWatcher;
import com.landenlabs.all_encrypnotes.ui.LogIt;
import com.landenlabs.all_encrypnotes.ui.RenameDialog;
import com.landenlabs.all_encrypnotes.ui.SliderDialog;
import com.landenlabs.all_encrypnotes.ui.UiSplashScreen;
import com.landenlabs.all_encrypnotes.ui.UiUtil;
import com.landenlabs.all_encrypnotes.ui.WebDialog;
import com.landenlabs.all_encrypnotes.ui.YesNoDialog;
import com.landenlabs.all_encrypnotes.util.GoogleAnalyticsHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encrypted Notepad based off work from Ivan Voras
 *
 * @author Dennis Lang <br>
 * WebSite: {@link http://home.comcast.net/~lang.dennis/}
 * <p>
 * @author Ivan Voras <br>
 * WebSite: {@link http://sourceforge.net/projects/enotes/}
 * @version 1.3
 * @see <a href="https://landenlabs.com">https://landenlabs.com</a>
 * @since 2014-Nov-25
 */
@SuppressWarnings({"JavadocReference", "Convert2Lambda", "unused", "ConstantConditions"})
public class EncrypNotes extends Activity implements DlgClickListener, OnSeekBarChangeListener {

    // -- Internal helper objects
    private EncrypPrefs m_prefs; // = new EncrypPrefs(this);
    private DocFileDlg m_docFileDialog; // = new DocFileDlg(this);
    private UiSplashScreen m_splashScreen; // = new UiSplashScreen(this);
    private HomeWatcher m_homeWatcher; // = new HomeWatcher(this);

    // -- UI view objects
    private float m_mainTextSize;
    private View m_titleBar;
    private ScrollView m_mainScroll;
    private EditText m_mainText;
    private MenuItem m_menuParanoid;
    private MenuItem m_menuGlobalPwd;
    private MenuItem m_menuInvertBg;

    private static final int PERMISSION_REQUEST_MEDIA = 100;

    private static final int HNDMSG_LOAD_DONE = 1;
    private static final int HNDMSG_SAVE_DONE = 2;
    private static final int HNDMSG_DRIVE_LIST = 3;
    private final Handler m_handler = new Handler() {

        public void handleMessage(Message msg) {

            switch (msg.what) {
                case HNDMSG_LOAD_DONE:
                    updateTitle();
                    break;
                case HNDMSG_SAVE_DONE:
                    //noinspection DuplicateBranchesInSwitch
                    updateTitle();
                    break;
                case HNDMSG_DRIVE_LIST:
                    break;
            }
        }
    };

    // Send message when Document file load is done  (or fails, see arg1 for state).
    public class SendLoadDoneMsg implements SendMsg {
        public void send(int msgNum) {
            m_handler.sendMessage(m_handler.obtainMessage(HNDMSG_LOAD_DONE, msgNum, 0));
        }
    }

    // Send message when Document file save is done (or fails, see arg1 for state).
    public class SendSaveDoneMsg implements SendMsg {
        public void send(int msgNum) {
            m_handler.sendMessage(m_handler.obtainMessage(HNDMSG_SAVE_DONE, msgNum, 0));
        }
    }

    private final SendLoadDoneMsg mSendLoadDoneMsg = new SendLoadDoneMsg();
    private final SendSaveDoneMsg mSendSaveDoneMsg = new SendSaveDoneMsg();

    // ========================================================================
    // Activity overrides

    /**
     * Creation of activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        init();

        // Prevent screen capture, must call before super.onCreate()
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        boolean DEBUG = (getApplicationInfo().flags & 2) != 0;
        // new Util.UncaughtExceptionHandler();
        GoogleAnalyticsHelper mAnalytics = new GoogleAnalyticsHelper(getApplication(), DEBUG);

        m_mainText = this.findViewById(R.id.main_text);
        m_mainTextSize = m_mainText.getTextSize();
        m_mainScroll = this.findViewById(R.id.main_scroll);

        LogIt.setDebugMode(getApplicationInfo());

        if (Util.fileExists(EncrypPrefs.PREFS_FILENAME)) {
            loadPrefs();
        }

        if (isMediaPermissionsGranted(this)) {
            ensureDocDir();
        } else {
            checkMediaPermissions(this, PERMISSION_REQUEST_MEDIA);
        }
        updateTitle();
        setupUI();

        //
        m_splashScreen.show();
    }

    // ---------------------------------------------------------------------------------------------
    /**
     */

    private static final String PREF_ACCOUNT_NAME = "landenlabs"; // @gmail.com";
    GoogleAccountCredential credential;
    // com.google.api.services.tasks.Tasks service;
    Drive driveService;
    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    static final int REQUEST_AUTHORIZATION = 1;
    static final int REQUEST_ACCOUNT_PICKER = 2;
    static final int REQUEST_SIGN_IN = 3;


    private void getDriveService()  {
        if (driveService == null) {
            // credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(TasksScopes.TASKS));
            getCredentials3(this);

            // service = new com.google.api.services.tasks.Tasks.Builder(httpTransport, jsonFactory, credential)
            //                .setApplicationName("EncrypNotes/1.0").build();
            driveService = new Drive.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName("EncrypNotes/1.0").build();
        }
    }


    /**
     * Starts the sign-in process and initializes the Drive client.
     */
    private static final Scope SCOPE_FILE = new Scope("https://www.googleapis.com/auth/drive.file");
    private static final Scope SCOPE_APPFOLDER = new Scope("https://www.googleapis.com/auth/drive.appdata");
    private static final int REQUEST_CODE_SIGN_IN = 123;

    private void signIn() {
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(SCOPE_FILE);
        requiredScopes.add(SCOPE_APPFOLDER);
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            getDriveService();
            testGoogleDrive();
        } else {
            startSignInIntent();
        }
    }


    private  GoogleAccountCredential getCredentials3(Context context) {
        if (credential == null) {
            GoogleAccountManager accountManager = new GoogleAccountManager(context);
            String email = accountManager.getAccounts().length > 0 ? accountManager.getAccounts()[0].name : PREF_ACCOUNT_NAME;

            // https://stackoverflow.com/questions/41219555/googleaccountcredential-error-the-name-must-not-be-empty-null-despite-per
            // List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);
            //   Collections.singleton(TasksScopes.TASKS));
            List<String> scopes = Arrays.asList(SCOPE_FILE.getScopeUri());
            // List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY);

            credential =
                    GoogleAccountCredential.usingOAuth2(context, scopes)
                            .setBackOff(new ExponentialBackOff())
                            // .setSelectedAccountName("wsimobile1@gmail.com");
                            // .setSelectedAccountName("landenlabs@gmail.com");
                            .setSelectedAccountName(email);
            SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
            credential.setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, email));
        }
        return credential;
    }

    private void chooseAccount() {
        credential = getCredentials3(getApplicationContext());
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private void startSignInIntent() {
        credential = getCredentials3(getApplicationContext());

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            getDriveService();
            testGoogleDrive();
            return;
        }

        /*
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(SCOPE_FILE)
                        .requestScopes(SCOPE_APPFOLDER)
                        .setAccountName(PREF_ACCOUNT_NAME)
                        .build();
        */

        // https://console.cloud.google.com/apis/api/drive.googleapis.com/credentials?project=all-encrypnotes-1578971821562
        // Google Drive key
        //            AIzaSyA5WQn5ks76SMfhuAv9axZ__hFDZitTKoI
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        // web
                //         .requestIdToken("902080584122-c4m2sr0i9h765fq30eege5s12021064c.apps.googleusercontent.com")
                //        .requestServerAuthCode("902080584122-c4m2sr0i9h765fq30eege5s12021064c.apps.googleusercontent.com")
                        // ??
                //        .requestIdToken("814289292845-4iouldk9rap52hmvo9r6mglf8k45kpgd.apps.googleusercontent.com")
                //        .requestServerAuthCode("814289292845-4iouldk9rap52hmvo9r6mglf8k45kpgd.apps.googleusercontent.com")
                        // android
                //        .requestIdToken("902080584122-9fboqjfq0vbkito9pku4qafo16ieoeju.apps.googleusercontent.com")
                //        .requestServerAuthCode("902080584122-9fboqjfq0vbkito9pku4qafo16ieoeju.apps.googleusercontent.com")
                        .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
        startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_SIGN_IN);
        /*
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions)
                .build();
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, REQUEST_SIGN_IN);
         */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (true || resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_GOOGLE_PLAY_SERVICES:
                    if (resultCode == Activity.RESULT_OK) {
                        // haveGooglePlayServices();
                    } else {
                        // checkGooglePlayServicesAvailable();
                    }
                    break;
                case REQUEST_AUTHORIZATION:
                    if (resultCode == Activity.RESULT_OK) {
                        // AsyncLoadTasks.run(this);
                    } else {
                        chooseAccount();
                    }
                    break;
                case REQUEST_ACCOUNT_PICKER:
                    if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                        String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                        if (accountName != null) {
                            credential.setSelectedAccountName(accountName);
                            SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString(PREF_ACCOUNT_NAME, accountName);
                            editor.commit();
                            // AsyncLoadTasks.run(this);
                            signIn();
                        }
                    }
                    break;
                case REQUEST_SIGN_IN:
                    // The Task returned from this call is always completed, no need to attach
                    // a listener.
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                    if (account != null) {
                        getDriveService();
                        testGoogleDrive();
                        return;
                    }
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        account = task.getResult(ApiException.class);

                        // Signed in successfully, show authenticated UI.
                        getDriveService();
                        testGoogleDrive();
                    } catch (ApiException e) {
                        // The ApiException status code indicates the detailed failure reason.
                        // Please refer to the GoogleSignInStatusCodes class reference for more information.

                        // https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes.html#INVALID_ACCOUNT
                        if (CommonStatusCodes.INVALID_ACCOUNT == e.getStatusCode()) {
                            LogIt.w("Den", "invalid account " + e.getMessage());
                        } else {
                            LogIt.w("Den", "signInResult:failed code=" + e.getStatusCode());
                        }
                    }

                    /*
                    GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
                    if (signInAccount != null) {
                        LogIt.d("DEN", signInAccount.toString());
                        getDriveService();
                        testGoogleDrive();
                    } else {
                        chooseAccount();
                    }
                     */
                    break;
            }
        } else {
            LogIt.e("Den", "failed");
        }
    }

    Thread driveListThread;

    void testGoogleDrive() {
        if (driveListThread != null) {
            driveListThread.stop();
        }
        driveListThread = new Thread(getDriveListRunnable);
        driveListThread.start();
    }

    Runnable getDriveListRunnable = new Runnable() {
        @Override
        public void run() {
            getDriveList();
        }
    };

    List<File> driveFiles;
    void getDriveList() {
        try {
            // Print the names and IDs for up to 10 files.
            FileList result = driveService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            driveFiles = result.getFiles();
            if (driveFiles == null || driveFiles.isEmpty()) {
                System.out.println("No files found.");
            } else {
                System.out.println("Files:");
                for (File file : driveFiles) {
                    System.out.printf("%s (%s)\n", file.getName(), file.getId());
                }
                m_handler.sendEmptyMessage(HNDMSG_DRIVE_LIST);
            }
        } catch (Exception ex) {
            Log.e("Den", ex.getMessage());
        }
    }

    /*
    // ---------------------------------------------------------------------------------------------
    class OnReady implements  FcmUtil.OnReady {

        private final String TAG = "FCM ready";
        @Override
        public void onSuccess() {
            Log.d(TAG, "FCM login  ");
        }

        @Override
        public void onFailure(String what, Exception why) {
            Log.e(TAG, "FCM login failed " + what + why.getMessage(), why);
        }
    }
    OnReady onReady = new OnReady();
    FcmUtil fcmUtil;
    DriveUtils driveUtils;
     */

    void init() {
        m_prefs = new EncrypPrefs(this);
        m_docFileDialog = new DocFileDlg(this);
        m_splashScreen = new UiSplashScreen(this);
        m_homeWatcher = new HomeWatcher(this);

        try {
            // testGoogleDrive();
            signIn();
        } catch (Exception ex) {
            LogIt.e("Den", ex.getMessage());
        }
        /*
        fcmUtil = new FcmUtil(this.getApplicationContext(), onReady);
        driveUtils = new DriveUtils(getApplicationContext());
         */
    }

    // =============================================================================================
    // Start Permissions

    /**
     * @return true if OS requires checking of permissions.
     */
    private static boolean doCheckPermissions() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Check media permission for SDK 23+
     */
    private static boolean isMediaPermissionsGranted(Context context) {
        if (doCheckPermissions()) {
            boolean isWrite = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean isRead = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            return isWrite && isRead;
        }
        return true;
    }

    /**
     * Check and request if needed Media permission for SDK 23+
     * Use this method to handle onRequestPermissionsResult in activity
     */
    @SuppressWarnings("SameParameterValue")
    private static void checkMediaPermissions(Activity activity, int requestCode) {
        if (doCheckPermissions()) {
            if (!isMediaPermissionsGranted(activity)) {
                final String[] wantPermissions =
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(activity, wantPermissions, requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults != null && grantResults.length > 0) {
            // requestCode (see code in Fragment.onRequestPermissionsResult)
            //    Upper 16 bits are used by Activity to store index to fragment
            //    Mask lower 16bits to get correct requestCode.
            int fragIndex = (requestCode >> 16) & 0xffff;

            if (fragIndex != 0) {
                // Calling up to super will call onRequestPermissionsResult on the active fragment.
                // Note - no clue if fragment handles the permission result.
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            } else {
                // No activie fragment, handle request permission here.
                requestCode = requestCode & 0xffff;
                if (requestCode == PERMISSION_REQUEST_MEDIA) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // Got permission - all is good
                    } else {
                        // Failed to get permission - we are in trouble
                    }
                    ensureDocDir();
                }
            }
        }
    }

    // End permissions
    // =============================================================================================

    /**
     * Create option menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);

        // Save off checkable menu items.
        m_menuParanoid = menu.findItem(R.id.menu_paranoid);
        m_menuGlobalPwd = menu.findItem(R.id.menu_global_pwd);
        m_menuInvertBg = menu.findItem(R.id.menu_invert);

        updateMenu();

        return super.onCreateOptionsMenu(menu);
    }
    
/* *****************
    // ** See associated code in DocFileDlg [context_menu]
    // Context menu on open file list does not work.

    / **
     * Create context menu - Load File list (delete, etc)
     * /
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle("Options");
        mode.getMenuInflater().inflate(R.menu.file_context_menu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        if (view.getTag() != null
                && view.getClass().getClass() == ListView.class.getClass()
                && (Integer)view.getTag() == DocFileDlg.FILE_CONTEXT_MENU) {
            ListView listView = (ListView)view;

            // Brute force - getSelectedItem();
            Object obj = null;
            for (int idx = 0; idx != listView.getChildCount(); idx++) {
                if (listView.getChildAt(idx).isSelected()) {
                    obj = listView.getAdapter().getItem(idx);
                    break;
                }
            }

            if (obj != null) {
                String selItem = obj.toString();
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.file_context_menu, menu);
                menu.setHeaderTitle(selItem);
            }
        }
    }
    
   / **
     * Long click on file load list - context menu 
     * http://stackoverflow.com/questions/9211545/android-context-menu-listview-for-files
     * This does not fire - web says it may go to onMenuItemSelected
     * /
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.file_delete:
                // File file = new File(m_storageDir + m_contextListView.getAdapter().getItem(info.position).toString() + DOC_EXT);
                // boolean deleted = file.delete();
                return true;
        }

        return super.onContextItemSelected(item);
    }
 
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
        case R.id.file_delete:
            // File file = new File(m_storageDir + m_contextListView.getAdapter().getItem(info.position).toString() + DOC_EXT);
            // boolean deleted = file.delete();
            return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }

 ***************** */

    /**
     * Handle option menu selections.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_new:
                newFile();
                return true;

            case R.id.menu_open:
                loadFile();
                return true;

            case R.id.menu_save:
                saveFileUI(DocFileDlg.SAVE);
                return true;

            case R.id.menu_save_as:
                saveFileUI(DocFileDlg.SAVE_AS);
                return true;

            case R.id.menu_email:
                if (m_mainText.getText().length() == 0) {
                    WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "<h2>Nothing to save</h2>");
                    return false;
                }
                Email.send(this, "to@gmail.com", "EncrypNotes", m_mainText.getText().toString());
                return true;

            case R.id.menu_about:
                m_splashScreen.show();
                aboutBox();
                return true;

            case R.id.menu_paranoid:
                m_prefs.Paranoid = !m_prefs.Paranoid;
                item.setChecked(m_prefs.Paranoid);
                return true;

            case R.id.menu_global_pwd:
                // m_prefs.Global_pwd_state = !m_prefs.Global_pwd_state;
                // item.setChecked(m_prefs.Global_pwd_state);
                // if (item.isChecked()) {
                getGlobalPwdState(item);
                // }
                return true;

            case R.id.menu_file_browser:
                openFileBrowser();
                return true;

            case R.id.menu_invert:
                m_prefs.InvertBg = !m_prefs.InvertBg;
                item.setChecked(m_prefs.InvertBg);
                updateBg();
                return true;

            case R.id.menu_zoom:
                SliderDialog sliderDlg = SliderDialog.create(R.layout.font_zoom_dlg, PRGMSG_FONT_ZOOM);
                sliderDlg.show(getFragmentManager(), "font_zoom");
                return true;

            case R.id.menu_copy:
            case R.id.menu_paste:
            case R.id.menu_cut:
            case R.id.menu_clear:
                ClipboardManager cMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                switch (item.getItemId()) {
                    case R.id.menu_copy:
                        if (m_mainText.length() != 0) {
                            ClipData clip = ClipData.newPlainText("simple text", m_mainText.getText());
                            cMan.setPrimaryClip(clip);
                            YesNoDialog.showOk(this, "Copied " + m_mainText.length() + " characters");
                        }
                        return true;

                    case R.id.menu_paste:
                        if (cMan.getPrimaryClip() != null && cMan.getPrimaryClip().getItemCount() != 0) {
                            String pasteStr = cMan.getPrimaryClip().getItemAt(0).getText().toString();
                            int beg = m_mainText.getSelectionStart();
                            int end = m_mainText.getSelectionEnd();
                            if (beg != -1 && end != -1) {
                                m_mainText.setText(m_mainText.getText().replace(beg, end, pasteStr));
                            } else {
                                m_mainText.setText(m_mainText.getText() + pasteStr);
                            }
                        }
                        return true;

                    case R.id.menu_cut:
                        if (m_mainText.length() != 0) {
                            int beg = m_mainText.getSelectionStart();
                            int end = m_mainText.getSelectionEnd();
                            beg = (beg == -1) ? 0 : beg;
                            end = (end == -1) ? m_mainText.length() : end;
                            ClipData clip = ClipData.newPlainText("simple text", m_mainText.getText()
                                    .subSequence(beg, end));
                            cMan.setPrimaryClip(clip);
                            m_mainText.setText(m_mainText.getText().replace(beg, end, ""));
                        }
                        return true;

                    case R.id.menu_clear:
                        if (m_mainText.length() != 0) {
                            ClipData clip = ClipData.newPlainText("simple text", m_mainText.getText());
                            cMan.setPrimaryClip(clip);
                            m_mainText.setText("");
                        }

                        return true;
                }

            case R.id.menu_info:
                m_docFileDialog.showInfo();
                break;
        }
        return false;
    }

    private static int m_startDepthCnt = 0;

    @Override
    protected void onStart() {
        super.onStart();
        new Util.UncaughtExceptionHandler();
        m_startDepthCnt++;

        m_homeWatcher.startWatch();
    }

    @Override
    public void onStop() {
        m_startDepthCnt--;
        m_homeWatcher.stopWatch();
        super.onStop();
    }

    @Override
    public void onPause() {
        saveIfNeeded(false);
        m_prefs.save();

        // Clear screen before saving - so thumbnail does not contain text.
        // This does not seem to work, so used FLAG_SECURE in onCreate()
        m_mainText.setVisibility(View.INVISIBLE);
        super.onPause();
    }

    @Override
    public void onResume() {
        m_mainText.setVisibility(View.VISIBLE);
        super.onResume();
    }

    /*
     * The problem: already saved files can be saved since we know both the filename and the
     * password. But new documents which are not yet saved have no such data associated with them.
     * We shall thus save these documents in internal memory in plaintext and hope it is secure
     * enough. We must also kill this saved data when we finally save the document.
     */
    @Override
    public void onSaveInstanceState(Bundle bundle) {
        m_prefs.save();
        if (!m_prefs.Paranoid) {
            m_docFileDialog.saveInstanceState(bundle, m_mainText);
            saveIfNeeded(false);
        }

        super.onSaveInstanceState(bundle);
    }

    /*
     * Only called if starting app after os killed app (ex: too many backgrounded apps).
     * If not paranoid, restore state.
     */
    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        m_prefs.load();
        if (m_prefs.Paranoid)
            return;
        m_docFileDialog.restoreInstanceState(bundle, m_mainText);
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        String exitMsg = m_docFileDialog.isModified() ? "Exit without saving ?" : "Exit ?";
        YesNoDialog.showDialog(this, "", exitMsg, CLKMSG_EXIT, YesNoDialog.BTN_YES_NO);
        // WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "Exit ?");
        // TODO - change to webdialog, make it nicer exit dialog.
        // WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "<h2>Exit ?</h2>");
    }

// ========================================================================
    // DlgClickListener  overrides

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @Override
    public void onClick(DialogFragment dialog, int whichMsg) {
        switch (whichMsg) {
            case CLKMSG_EXIT:
                this.finish();  // Fatal error - time to exit or backpress.
                break;
            case -CLKMSG_EXIT:
                break;
            case CLKMSG_NEW_FILE:
                newFile();      // Save any existing, setup new file.
                break;
            case CLKMSG_SAVE_THEN_NEW:
                if (!saveIfNeeded(true))
                    return;
            case -CLKMSG_SAVE_THEN_NEW:
                m_docFileDialog.Clear();
                UiUtil.setText(m_mainText, "");
                updateTitle();
                break;
            case CLKMSG_SAVE_THEN_OPEN:
                if (!saveIfNeeded(true))
                    return;
            case -CLKMSG_SAVE_THEN_OPEN:
                m_docFileDialog.showLoad(m_prefs, m_mainText, mSendLoadDoneMsg);
                break;
            case CLKMSG_FILENAME_CHANGED:
                updateTitle();
                break;
            case R.id.file_delete: {
                YesNoDialog yesNoDialog = (YesNoDialog) dialog;
                String filename = (String) yesNoDialog.getValue();
                FileListAdapter fileListAdapter = (FileListAdapter) yesNoDialog.getViewer();
                fileListAdapter.deleteFile(filename);
            }
            break;
            case R.id.file_rename: {
                RenameDialog renameDialog = (RenameDialog) dialog;
                String fromFile = renameDialog.getFrom();
                String toFile = renameDialog.getTo();
                FileListAdapter fileListAdapter = (FileListAdapter) renameDialog.getViewer();
                fileListAdapter.renameFile(fromFile, toFile);
            }
            break;
            default:  // CLKMSG_NONE
                break;
        }
    }

    // ========================================================================
    // OnSeekBarChangeListener overrides
    // Font scale

    // OnProgress messages
    private static final int PRGMSG_FONT_ZOOM = 100;

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
        int value = arg0.getProgress(); // 0...100
        //noinspection SwitchStatementWithTooFewBranches
        switch (arg1) {
            case PRGMSG_FONT_ZOOM:
                m_prefs.TextScale = (value - 50) / 50.0f; // -1.0f ... +1.0f;
                updateTextSize();
        }
    }


    // ========================================================================
    // Activity main logic

    /**
     * Setup User interface, add callbacks.
     * + afterTextChanged (mainText)
     * + onTouch   (scrollView)
     * + onClick   (titleBar)
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupUI() {
        m_mainText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // See UiUtil.setText, trick set to disable to indicate no text watcher. 
                if (m_mainText.isEnabled()) {
                    if (!m_docFileDialog.isModified()) {
                        m_docFileDialog.setModified(true);
                        m_splashScreen.hide();
                    }
                    updateTitle();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // Hide soft keyboard if user is scrolling for awhile.
        m_mainScroll.setOnTouchListener(new OnTouchListener() {

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                long downDurationMillis = SystemClock.uptimeMillis() - event.getDownTime();
                if (event.getAction() == MotionEvent.ACTION_MOVE && downDurationMillis > 300) {
                    UiUtil.hideSoftKeyboard(getCurrentFocus());
                }
                return false;
            }
        });

        // Hide soft keyboard if user clicks on action/title bar.
        if (m_titleBar != null)
            m_titleBar.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    UiUtil.hideSoftKeyboard(getCurrentFocus());
                }
            });

    }

    /**
     * Load Preferences saved
     *
     * @return {@code true} if successful
     */
    @SuppressWarnings({"UnusedReturnValue", "SameReturnValue"})
    private boolean loadPrefs() {
        m_prefs.load();
        updateBg();
        updateTextSize();
        updateMenu();
        return true;
    }

    /**
     * Set dark or lite background.
     */
    private void updateBg() {
        if (m_prefs.InvertBg) {
            m_mainText.setBackground(getResources().getDrawable(R.drawable.paper_dark));
            m_mainText.setTextColor(Color.WHITE);
        } else {
            m_mainText.setBackground(getResources().getDrawable(R.drawable.paper_lite));
            m_mainText.setTextColor(Color.BLACK);
        }
    }

    /**
     * Set text size using text scale.
     */
    private void updateTextSize() {
        float adjSize = m_mainTextSize / 2 * m_prefs.TextScale;
        m_mainText.setTextSize(m_mainTextSize + adjSize);
    }

    /**
     * Update menu to match preferences.
     */
    private void updateMenu() {
        m_menuParanoid.setChecked(m_prefs.Paranoid);
        m_menuGlobalPwd.setChecked(m_prefs.Global_pwd_state);
        m_menuInvertBg.setChecked(m_prefs.InvertBg);
    }

    /**
     * Ensure document directory exists.
     */
    private void ensureDocDir() {
        if (m_docFileDialog.ensureDocDir())
            return;
        YesNoDialog.showOk(this, "Cannot make directory " + DocFileDlg.getDir().getName()
                + "\nEnable write permissions.", CLKMSG_EXIT);
    }

    /**
     * Save active file if needed (does not prompt).
     *
     * @return False is need save but unable.
     */
    private boolean saveIfNeeded(boolean isVisible) {
        boolean saved = true;
        if (m_docFileDialog.isModified()) {
            if (isVisible) {
                if (m_docFileDialog.canSave()) {
                    m_docFileDialog.saveFile(null, null, null, m_mainText, mSendSaveDoneMsg);
                } else {
                    WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "Need filename, Use <b>Save As</b> to save");
                    saved = false;
                }
            } else {
                LogIt.log(this.getClass().getSimpleName(), LogIt.WARN, "Going background - unsaved text");
                m_docFileDialog.saveOnBackground("quick_save", m_prefs.Global_pwd_value, m_prefs.Global_pwd_hint, m_mainText);
            }
        }

        return saved;
    }

    /**
     * Prompt to save any active file, then clear buffer for new file.
     */
    private void newFile() {
        if (m_docFileDialog.isModified() && m_mainText.length() != 0) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            YesNoDialog yesNoDialog = YesNoDialog.create("Save Changes", "Save changes ?", CLKMSG_SAVE_THEN_NEW, YesNoDialog.BTN_YES_NO);
            yesNoDialog.show(ft, "msgDialog");
        } else {
            m_docFileDialog.Clear();
            UiUtil.setText(m_mainText, "");
            updateTitle();
        }
    }

    private void loadFile() {
        if (m_docFileDialog.isModified() && m_mainText.length() != 0) {
            YesNoDialog.showDialog(this, "Save Changes", "Save changes ?", CLKMSG_SAVE_THEN_OPEN, YesNoDialog.BTN_YES_NO);
            // FragmentTransaction ft = getFragmentManager().beginTransaction();
            // YesNoDialog yesNoDialog = YesNoDialog.create("Save Changes", "Save changes ?", CLKMSG_SAVE_THEN_OPEN, YesNoDialog.BTN_YES_NO);
            // yesNoDialog.show(ft, "newSaveDialog");
        } else {
            m_docFileDialog.showLoad(m_prefs, m_mainText, mSendLoadDoneMsg);
        }
    }

    /**
     * Prompt for file to save text to.
     *
     * @param saveMode SAVE or SAVE_AS
     */
    private void saveFileUI(int saveMode) {

        if (m_mainText.getText().length() == 0) {
            WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "<h2>Nothing to save</h2>");
            return;
        }

        if (saveMode == DocFileDlg.SAVE) {
            if (!m_docFileDialog.isModified()) {
                WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "<h2>Already saved, nothing has changed</h2>");
                return;
            }

            if (m_docFileDialog.canSave()) {
                // Save with current filename and pwd.
                m_docFileDialog.saveFile(null, null, null, m_mainText, mSendSaveDoneMsg);
                return;
            }
        }

        m_docFileDialog.showSaveAs(m_prefs, DocFileDlg.SAVE_AS, CLKMSG_FILENAME_CHANGED, m_mainText, mSendSaveDoneMsg);
    }


    /**
     * Show about information in dialog box.
     * Use html web viewer in AlertDialog.
     */
    private void aboutBox() {
        // wv.loadUrl("file:///android_asset/about.html");
        String htmlStr = String.format(UiUtil.LoadData(this, "about.html"), UiUtil.getPackageInfo(this).versionName,
                Doc.CRYPTO_MODE);
        WebDialog.show(this, WebDialog.HTML_CENTER_BOX, htmlStr);
    }

    /* Shows a simple about box dialog */
    @SuppressWarnings("unused")
    private void optionsBox() {
        final Dialog dlg = new Dialog(this);
        dlg.setContentView(R.layout.globalpwd_state_dlg);
        dlg.setTitle(R.string.dlg_prefs_title);
        dlg.setCancelable(true);

        Button btn_ok =  dlg.findViewById(R.id.dp_btn_ok);
        btn_ok.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                dlg.dismiss();
            }
        });
        dlg.show();
    }

    /**
     * Manage global password.
     * TODO - complete code.
     */
    private void getGlobalPwdState(final MenuItem item) {
        final Dialog dlg = new Dialog(this);
        dlg.setContentView(R.layout.globalpwd_state_dlg);
        dlg.setTitle(R.string.dlg_prefs_title);
        dlg.setCancelable(true);

        Button btn_ok = dlg.findViewById(R.id.dp_btn_ok);
        btn_ok.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                m_prefs.Global_pwd_state = true;
                item.setChecked(m_prefs.Global_pwd_state);
                m_prefs.save();
                dlg.dismiss();
                getGlobalPwdValue();
            }
        });

        Button btn_cancel = dlg.findViewById(R.id.dp_btn_cancel);
        btn_cancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                m_prefs.Global_pwd_state = false;
                item.setChecked(m_prefs.Global_pwd_state);
                m_prefs.save();
                dlg.dismiss();
            }
        });

        dlg.show();
    }

    /**
     * Manage global password.
     * TODO - complete code.
     */
    private void getGlobalPwdValue() {
        final Dialog dlg = new Dialog(this);
        dlg.setContentView(R.layout.globalpwd_value_dlg);
        dlg.setTitle(R.string.set_global_password);
        dlg.setCancelable(true);

        final UiPasswordManager managePwd = new UiPasswordManager(m_prefs, dlg, true);
        managePwd.getPwdView().setHint(R.string.global_password);
        if (!TextUtils.isEmpty(m_prefs.Global_pwd_hint))
            managePwd.setHint(m_prefs.Global_pwd_hint);

        Button btn_ok = dlg.findViewById(R.id.dp_btn_ok);
        btn_ok.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                // EditText hintText = UiUtil.viewById(dlg, R.id.pwd_hint_value);
                EditText pwdText = managePwd.getPwdView();

                m_prefs.Global_pwd_hint = managePwd.getHint();
                m_prefs.Global_pwd_value = pwdText.getText().toString();
                m_prefs.Global_pwd_state = true;
                m_prefs.save();
                dlg.dismiss();
            }
        });

        Button btn_cancel = dlg.findViewById(R.id.dp_btn_cancel);
        btn_cancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                dlg.dismiss();
            }
        });

        dlg.show();
    }


    // Does not fully work - allows file browser to open, but does not
    // default to our directory.
    private void openFileBrowser() {
        Uri selectedUri = Uri.parse(DocFileDlg.getDir().getAbsolutePath());
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        // intent.putExtra(Intent.EXTRA_CONTENT_QUERY, DocFileDlg.getDir().getAbsolutePath());
        // intent.putExtra(Intent.EXTRA_CONTENT_QUERY, "FILE");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(DocFileDlg.MIME_TYPE);
        // intent.setDataAndType(selectedUri, DocFileDlg.MIME_TYPE);
        startActivity(intent);
        Toast.makeText(this, selectedUri.getPath(), Toast.LENGTH_LONG).show();
        //  startActivity(Intent.createChooser(intent, "Open folder"));
    }

    /**
     * Set app title to open filename, red indicates text has been modified.
     */
    private void updateTitle() {
        final String fname = TextUtils.isEmpty(m_docFileDialog.getName()) ?
                getResources().getString(R.string.new_document) :
                m_docFileDialog.getName();

        final int lineCnt = m_mainText.getLineCount();
        final int charCnt = m_mainText.length();
        this.setTitle(getResources().getString(R.string.app_title, fname, charCnt));

        if (m_titleBar == null) {
            // Setting background color depends on whether action bar is enabled
            int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            m_titleBar = findViewById(titleId);
            if (m_titleBar == null) {
                View title = getWindow().findViewById(android.R.id.title);
                m_titleBar = (View) title.getParent();
            }

            // Allow clicking on title bar to hide keyboard.
            if (m_titleBar != null) {
                m_titleBar.setFocusableInTouchMode(true);
            }
        }

        if (m_titleBar != null) {
            if (m_docFileDialog.isModified())
                m_titleBar.setBackgroundColor(Color.RED);
            else
                m_titleBar.setBackgroundColor(Color.BLACK);
        }
    }

    // ========================== GOOGLE DRIVE ================================

}
