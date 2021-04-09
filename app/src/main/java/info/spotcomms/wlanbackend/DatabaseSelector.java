package info.spotcomms.wlanbackend;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class DatabaseSelector extends Activity {

    private static final int PICK_WPSDB_FILE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.database_selector);
        openFilePicker(PICK_WPSDB_FILE);
        showToast(getString(R.string.select_db), Toast.LENGTH_LONG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == PICK_WPSDB_FILE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                File realDB = new File(getApplicationContext().getFilesDir(), "WPSDB.csv");
                try {
                    FileInputStream selectedDB = (FileInputStream) getContentResolver().openInputStream(uri);
                    copyFileUsingChannel(selectedDB, realDB);
                    showToast(getString(R.string.selected_db), Toast.LENGTH_LONG);
                    finish();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void openFilePicker(int result) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, result);
    }

    //Credit (CC-BY-SA 4.0): https://stackoverflow.com/a/32652909
    private static void copyFileUsingChannel(FileInputStream source, File dest) throws IOException {
        if (dest.exists()) {
            dest.delete();
        }
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = source.getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            sourceChannel.close();
            destChannel.close();
        }
    }

    private void showToast(String text, int duration) {
        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        toast.show();
    }
}