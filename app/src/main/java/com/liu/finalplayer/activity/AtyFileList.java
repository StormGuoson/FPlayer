package com.liu.finalplayer.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.liu.finalplayer.R;
import com.liu.finalplayer.adapter.MyCursorAdapter;
import com.liu.finalplayer.database.FileDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import static com.liu.finalplayer.database.DbContent.DATABASE_NAME;
import static com.liu.finalplayer.database.DbContent.DATE;
import static com.liu.finalplayer.database.DbContent.DIR;
import static com.liu.finalplayer.database.DbContent.DataBYTE;
import static com.liu.finalplayer.database.DbContent.DataWATCH;
import static com.liu.finalplayer.database.DbContent.FILE;
import static com.liu.finalplayer.database.DbContent.LAST;
import static com.liu.finalplayer.database.DbContent.SIZE;
import static com.liu.finalplayer.database.DbContent.TABLE_NAME;
import static com.liu.finalplayer.database.DbContent.TITLE;
import static com.liu.finalplayer.database.DbContent.WATCH;


/**
 * Created by StormGuoson on 2017/1/9.
 */

public class AtyFileList extends AppCompatActivity implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    MenuItem item;                                           //扫描菜单按键
    View view;                                               //搜索栏布局
    ActionBar actionBar;                                     //菜单栏
    ArrayAdapter<String> searchAdapter;                      //搜索
    ArrayList<String> list;                                  //搜索
    AutoCompleteTextView autoCompleteTextView;               //搜索提示
    ImageView iv_actionBar_Back;                             //搜索退出
    boolean isActionBarSearchMode = false;                   //搜索模式
    SimpleDateFormat format;                                 //格式化日期
    FileDatabase database;                                   //数据库
    SQLiteDatabase write;
    ListView listView;                                       //视频列表
    Cursor cursor;
    MyCursorAdapter cursorAdapter;
    String data[] = new String[]{TITLE, SIZE, DIR, WATCH, DATE};
    int dataId[] = new int[]{R.id.tvTitle, R.id.tvSize, R.id.tvDir, R.id.tvHasWatch, R.id.tvAddDate};
    AlertDialog.Builder netAlert;
    Dialog orderDialog, netDialog;                              //排序dialog,网络视频dialog
    CheckBox cbAsc;                                      //升降序
    RadioButton rbTitle, rbSize;                      //排序按钮
    TextView tvHint;                                            //0视频提示
    File dir = new File("/mnt/sdcard");                         //扫描目录
    int addFile = 0;                                            //添加视频数
    boolean isOver = true;                                      //扫描结束

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lay_file_list);
        setTitle(getString(R.string.vList));
        createOrderDialog();
        initDatabase();
        initButton();
        initOptions();
        enterToSearch();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cursor.requery();
        cursorAdapter.notifyDataSetChanged();
    }

    /**
     * 初始化数据库并检测视频文件
     */
    void initDatabase() {
        list = new ArrayList<>();
        listView = (ListView) findViewById(R.id.lvFileList);
        database = new FileDatabase(this, DATABASE_NAME, null, 1);
        write = database.getWritableDatabase();
        cursor = write.query(TABLE_NAME, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            list.add(cursor.getString(cursor.getColumnIndex(TITLE)));
            String path = cursor.getString(cursor.getColumnIndex(FILE));
            File file = new File(path);
            if (!file.exists()) {
                write.delete(TABLE_NAME, FILE + "=?", new String[]{path});
            }
        }
        cursor = write.query(TABLE_NAME, null, null, null, null, null, null);
        cursorAdapter = new MyCursorAdapter(this, R.layout.cell_file_list, cursor, data, dataId);
        listView.setAdapter(cursorAdapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
    }

    /**
     * 控件
     */
    void initButton() {
        actionBar = getSupportActionBar();
        view = LayoutInflater.from(this).inflate(R.layout.menu_search, null);
        actionBar.setCustomView(view);
        iv_actionBar_Back = (ImageView) actionBar.getCustomView().findViewById(R.id.iv_bar_back);
        autoCompleteTextView = (AutoCompleteTextView) actionBar.getCustomView().findViewById(R.id.autoTv_bar_search);

        tvHint = (TextView) findViewById(R.id.tvScanHint);
        iv_actionBar_Back.setOnClickListener(this);
        searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        autoCompleteTextView.setAdapter(searchAdapter);

        if (cursorAdapter.getCount() == 0)
            tvHint.setVisibility(View.VISIBLE);
        else tvHint.setVisibility(View.GONE);
    }

    /**
     * 初始化配置
     */
    void initOptions() {
        preferences = getSharedPreferences("option", Context.MODE_PRIVATE);
        editor = preferences.edit();
        rbTitle.setChecked(preferences.getBoolean("title", true));
        rbSize.setChecked(preferences.getBoolean("size", false));
        cbAsc.setChecked(preferences.getBoolean("checkBox", true));
    }

    /**
     * 排序dialog
     */
    void createOrderDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_order, null);
        cbAsc = (CheckBox) view.findViewById(R.id.cbAsc);
        rbTitle = (RadioButton) view.findViewById(R.id.rbTitle);
        rbSize = (RadioButton) view.findViewById(R.id.rbSize);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        orderDialog = builder.create();
        rbTitle.setOnCheckedChangeListener(this);
        rbSize.setOnCheckedChangeListener(this);
        cbAsc.setOnCheckedChangeListener(this);
    }

    /**
     * 开始动画
     */
    void refreshAnim(MenuItem item) {
        this.item = item;
        ImageView view = new ImageView(this);
        view.setImageResource(R.drawable.ic_refresh_white_24dp);
        item.setActionView(view);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fresh);
        view.startAnimation(animation);
    }

    /**
     * 结束动画
     */
    void hideRefreshAnimation() {
        if (item != null) {
            ImageView view = (ImageView) item.getActionView();
            if (view != null) {
                view.clearAnimation();
                item.setActionView(null);
            }
        }
    }

    /**
     * 获取文件大小
     */
    String getFileSize(File file) {
        String fileSizeString = "";
        int fileS;
        try {
            DecimalFormat df = new DecimalFormat("#.0");
            FileInputStream stream = new FileInputStream(file);
            fileS = stream.available();
            if (fileS < 1024) {
                fileSizeString = df.format((double) fileS) + "B";
            } else if (fileS < 1048576) {
                fileSizeString = df.format((double) fileS / 1024) + "KB";
            } else if (fileS < 1073741824) {
                fileSizeString = df.format((double) fileS / 1048576) + "MB";
            } else {
                fileSizeString = df.format((double) fileS / 1073741824) + "GB";
            }
            return fileSizeString;
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return fileSizeString;
    }

    /**
     * 扫描文件
     */
    void searchFile() throws IOException, InterruptedException {
        String fileEnd;
        isOver = false;
        Cursor cursor = write.query(TABLE_NAME, new String[]{FILE}, null, null, null, null, null);
        String[] strings = new String[cursor.getCount()];
        int count = 0;
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                strings[count] = cursor.getString(cursor.getColumnIndex(FILE));
                ++count;
            }
        }
        count = 0;
        cursor.close();
        LinkedList<File> first = new LinkedList<>();
        LinkedList<File> second = new LinkedList<>();
        File[] child = dir.listFiles();
        for (File file : child) {
            fileEnd = file.getName().toLowerCase();
            if (file.isDirectory())
                first.add(file);
            else if (videoType(fileEnd)) {
                for (String title : strings)
                    if (file.getAbsolutePath().equals(title)) break;
                    else count++;
                if (count == strings.length) {
                    addVideo(file);
                    ++addFile;
                }
                count = 0;
            }
        }
        File temp;
        while (!first.isEmpty()) {
            temp = first.removeFirst();
            child = temp.listFiles();
            for (File file : child) {
                fileEnd = file.getName().toLowerCase();
                if (file.isDirectory())
                    second.add(file);
                else if (videoType(fileEnd)) {
                    for (String title : strings)
                        if (file.getAbsolutePath().equals(title)) break;
                        else count++;
                    if (count == strings.length) {
                        addVideo(file);
                        ++addFile;
                    }
                    count = 0;
                }
            }
        }
        while (!second.isEmpty()) {
            temp = second.removeFirst();
            child = temp.listFiles();
            for (File file : child) {
                fileEnd = file.getName().toLowerCase();
                if (file.isFile() && videoType(fileEnd)) {
                    for (String title : strings)
                        if (file.getAbsolutePath().equals(title)) break;
                        else count++;
                    if (count == strings.length) {
                        addVideo(file);
                        ++addFile;
                    }
                    count = 0;
                }
            }
        }
        isOver = true;
    }

    /**
     * 添加支持的视频格式
     */
    boolean videoType(String fileEnd) {
        return fileEnd.endsWith(".mp4") || fileEnd.endsWith(".avi") || fileEnd.endsWith(".rmvb") || fileEnd.endsWith(".flv")
                || fileEnd.endsWith(".3gp") || fileEnd.endsWith(".m4v") || fileEnd.endsWith(".mov") || fileEnd.endsWith(".mkv")
                || fileEnd.endsWith(".ts") || fileEnd.endsWith(".wmv") || fileEnd.endsWith(".asf") || fileEnd.endsWith(".divx")
                || fileEnd.endsWith(".dvr-ms") || fileEnd.endsWith(".f4v") || fileEnd.endsWith(".m2ts") || fileEnd.endsWith(".m3u")
                || fileEnd.endsWith(".m3u8") || fileEnd.endsWith(".mpeg") || fileEnd.endsWith(".mpg") || fileEnd.endsWith(".mts")
                || fileEnd.endsWith(".ogg") || fileEnd.endsWith(".ogm") || fileEnd.endsWith(".rm") || fileEnd.endsWith(".ts")
                || fileEnd.endsWith(".vob") || fileEnd.endsWith(".webm") || fileEnd.endsWith(".wtv");
    }

    /**
     * 添加视频到数据库
     */
    void addVideo(File file) throws IOException, InterruptedException {
        if (format == null)
            format = new SimpleDateFormat("M月d日", Locale.CHINA);
        ContentValues values = new ContentValues();
        String fileDir = file.getAbsolutePath();
        values.clear();
        values.put(FILE, fileDir);
        values.put(TITLE, file.getName().substring(0, file.getName().lastIndexOf(".")));
        values.put(DIR, fileDir.substring(11, fileDir.length()));
        values.put(SIZE, getFileSize(file));
        values.put(DataBYTE, new FileInputStream(file).available());
        values.put(DataWATCH, 0);
        values.put(LAST, 0);
        values.put(WATCH, "00:00");
        values.put(DATE, format.format(new Date()));
        write.insert(TABLE_NAME, null, values);
    }

    /**
     * 回车搜索
     */
    void enterToSearch() {
        autoCompleteTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    Cursor cursor = write.query(TABLE_NAME, null, TITLE + " like?", new String[]{"%" + autoCompleteTextView.getText().toString() + "%"}, null, null, TITLE + " asc");
                    CursorAdapter cursorAdapter = new MyCursorAdapter(AtyFileList.this, R.layout.cell_file_list, cursor, data, dataId);
                    listView.setAdapter(cursorAdapter);
                    autoCompleteTextView.setText("");
                    Toast.makeText(AtyFileList.this, R.string.searchOver, Toast.LENGTH_SHORT).show();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                }
                return true;
            }
        });
    }

    /**
     * 播放视频
     */
    void startVideo(Cursor cursor1) {
        String filePath = cursor1.getString(cursor1.getColumnIndex(FILE));
        String fileTitle = cursor1.getString(cursor1.getColumnIndex(TITLE));
        int fileWatch = cursor1.getInt(cursor1.getColumnIndex(DataWATCH));
        Intent intent = new Intent(AtyFileList.this, AtyMainVideo.class);
        intent.putExtra("video", filePath);
        intent.putExtra("title", fileTitle);
        intent.putExtra("watch", fileWatch);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isActionBarSearchMode)
            getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.muScan:
                if (isOver) {
                    refreshAnim(item);
                    AsyncTask<Void, String, Void> asyncTask = new AsyncTask<Void, String, Void>() {

                        @Override
                        protected void onCancelled() {
                            super.onCancelled();
                            hideRefreshAnimation();
                            Toast.makeText(AtyFileList.this, R.string.permissionFailed, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            Toast.makeText(AtyFileList.this, R.string.startScan, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Toast.makeText(AtyFileList.this, getString(R.string.scanOver1) + addFile + getString(R.string.scanOver2), Toast.LENGTH_SHORT).show();
                            addFile = 0;
                            hideRefreshAnimation();
                            cursor.requery();
                            cursorAdapter.notifyDataSetChanged();
                            if (cursorAdapter.getCount() != 0)
                                tvHint.setVisibility(View.GONE);
                            Cursor cursor1 = write.query(TABLE_NAME, new String[]{TITLE}, null, null, null, null, null);
                            list.clear();
                            while (cursor1.moveToNext()) {
                                list.add(cursor1.getString(cursor1.getColumnIndex(TITLE)));
                            }
                        }

                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                searchFile();
//                                testFile();
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    };
                    asyncTask.execute();
                } else Toast.makeText(this, R.string.repeat_warning, Toast.LENGTH_SHORT).show();
                break;
            case R.id.muOrder:
                orderDialog.show();
                break;
            case R.id.muSearch:
                if (isOver) {
                    isActionBarSearchMode = true;
                    view.setVisibility(View.VISIBLE);
                    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                    invalidateOptionsMenu();
                    autoCompleteTextView.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                } else Toast.makeText(this, R.string.searchAfterScan, Toast.LENGTH_SHORT).show();
                break;
            case R.id.muNet:
                final EditText editText;
                if (netAlert == null) {
                    netAlert = new AlertDialog.Builder(this);
                    View view = LayoutInflater.from(this).inflate(R.layout.dialog_net_video, null);
                    editText = (EditText) view.findViewById(R.id.etUrl);
                    netAlert.setView(view);
                    netAlert.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (!editText.getText().toString().equals("")) {
                                Intent intent = new Intent(AtyFileList.this, AtyMainVideo.class);
                                intent.putExtra("netVideo", editText.getText().toString());
                                startActivity(intent);
                            }
                        }
                    });
                    netAlert.setNegativeButton("取消", null);
                    netDialog = netAlert.create();
                }
                netDialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Cursor cursor1 = (Cursor) adapterView.getItemAtPosition(i);
        startVideo(cursor1);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.rbTitle:
                if (rbTitle.isChecked()) {
                    editor.putBoolean("title", true);
                    editor.putBoolean("size", false);
                    editor.commit();
                    if (cbAsc.isChecked())
                        cursor = write.query(TABLE_NAME, null, null, null, null, null, TITLE + " asc");
                    else
                        cursor = write.query(TABLE_NAME, null, null, null, null, null, TITLE + " desc");
                }
                orderDialog.dismiss();
                break;
            case R.id.rbSize:
                if (rbSize.isChecked()) {
                    editor.putBoolean("title", false);
                    editor.putBoolean("size", true);
                    editor.commit();
                    if (cbAsc.isChecked())
                        cursor = write.query(TABLE_NAME, null, null, null, null, null, DataBYTE + " asc");
                    else
                        cursor = write.query(TABLE_NAME, null, null, null, null, null, DataBYTE + " desc");
                }
                orderDialog.dismiss();
                break;
            case R.id.cbAsc:
                if (cbAsc.isChecked()) {
                    editor.putBoolean("checkBox", true);
                    editor.commit();
                    if (rbTitle.isChecked()) {
                        cursor = write.query(TABLE_NAME, null, null, null, null, null, TITLE + " asc");
                    } else if (rbSize.isChecked()) {
                        cursor = write.query(TABLE_NAME, null, null, null, null, null, DataBYTE + " asc");
                    }
                } else {
                    editor.putBoolean("checkBox", false);
                    editor.commit();
                    if (rbTitle.isChecked()) {
                        cursor = write.query(TABLE_NAME, null, null, null, null, null, TITLE + " desc");
                    } else if (rbSize.isChecked()) {
                        cursor = write.query(TABLE_NAME, null, null, null, null, null, DataBYTE + " desc");
                    }
                }
                break;
        }
        cursorAdapter = new MyCursorAdapter(this, R.layout.cell_file_list, cursor, data, dataId);
        listView.setAdapter(cursorAdapter);
        write.execSQL("UPDATE sqlite_sequence SET seq = 0 WHERE name = 'fileList'");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_bar_back:
                isActionBarSearchMode = false;
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
                listView.setAdapter(cursorAdapter);
                invalidateOptionsMenu();
                this.view.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (isActionBarSearchMode) {
            isActionBarSearchMode = false;
            autoCompleteTextView.setText("");
            invalidateOptionsMenu();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
            view.setVisibility(View.GONE);
            listView.setAdapter(cursorAdapter);
        } else
            super.onBackPressed();
    }
}
