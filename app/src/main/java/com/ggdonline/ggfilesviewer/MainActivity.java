package com.ggdonline.ggfilesviewer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQUEST_OPEN = 10;
    private static final int REQUEST_SAVE = 11;
    private EditText editor;
    private TextView status;
    private String currentType = "TEXT";
    private boolean openedExisting = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        if (getIntent() != null && Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            if (uri != null) readUri(uri);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(0xFFF5F7FB);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(26);
        title.setTextColor(0xFF202B42);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(58)));

        status = new TextView(this);
        status.setText(R.string.status_format_author);
        status.setTextSize(13);
        status.setTextColor(0xFF526078);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(34)));

        Button create = makeButton(getString(R.string.create_gg_file));
        create.setOnClickListener(v -> {
            editor.setText("");
            currentType = "TEXT";
            openedExisting = false;
            status.setText(getString(R.string.status_new_file, currentType));
            Toast.makeText(this, R.string.toast_enter_text_save, Toast.LENGTH_SHORT).show();
        });
        root.addView(create, buttonParams());

        Button open = makeButton(getString(R.string.open_gg_file));
        open.setOnClickListener(v -> openPicker());
        root.addView(open, buttonParams());

        editor = new EditText(this);
        editor.setHint(R.string.editor_hint);
        editor.setTextSize(16);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setPadding(dp(12), dp(12), dp(12), dp(12));
        editor.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams editorParams = new LinearLayout.LayoutParams(-1, 0, 1);
        editorParams.topMargin = dp(12);
        root.addView(editor, editorParams);

        Button save = makeButton(getString(R.string.save_gg_file));
        save.setOnClickListener(v -> savePicker());
        root.addView(save, buttonParams());

        setContentView(root);
    }

    private Button makeButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.topMargin = dp(8);
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void openPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQUEST_OPEN);
    }

    private void savePicker() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/octet-stream");
        i.putExtra(Intent.EXTRA_TITLE, "new_file.gg");
        startActivityForResult(i, REQUEST_SAVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_OPEN) {
            readUri(uri);
        } else if (requestCode == REQUEST_SAVE) {
            writeUri(uri);
        }
    }

    private void readUri(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new Exception(getString(R.string.error_open_file));
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            String raw = out.toString("UTF-8");
            currentType = "TEXT";
            String body = raw;
            if (raw.startsWith("GGFILE\n") || raw.startsWith("GGFILE\r\n")) {
                String[] lines = raw.split("\\r?\\n", -1);
                StringBuilder content = new StringBuilder();
                boolean inBody = false;
                for (String line : lines) {
                    if (inBody) content.append(line).append("\n");
                    else if (line.startsWith("TYPE=")) currentType = line.substring(5).trim();
                    else if (line.trim().isEmpty()) inBody = true;
                }
                body = content.toString();
            }
            editor.setText(body);
            openedExisting = true;
            status.setText(getString(R.string.status_opened_file, currentType));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_opening, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void writeUri(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new Exception(getString(R.string.error_save_file));
            String content = editor.getText().toString();
            String full;
            if (openedExisting && content.startsWith("GGFILE")) {
                full = content;
            } else {
                full = "GGFILE\nVERSION=2.0\nTYPE=" + currentType
                        + "\nAUTHOR=777GDONLINE\n\n" + content;
            }
            out.write(full.getBytes(StandardCharsets.UTF_8));
            out.flush();
            openedExisting = true;
            status.setText(getString(R.string.status_saved, currentType));
            Toast.makeText(this, R.string.toast_gg_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_saving, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
}
