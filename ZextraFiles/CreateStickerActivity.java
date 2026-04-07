package com.stickers.bank.ui.activity;

import static com.stickers.bank.utils.PermissionUtils.REQUEST_PERMISSION_READ_WRITE_STORAGE;
import static com.stickers.bank.utils.StickerHandler.EXTRA_STICKER_PACK_AUTHORITY;
import static com.stickers.bank.utils.StickerHandler.EXTRA_STICKER_PACK_ID;
import static com.stickers.bank.utils.StickerHandler.EXTRA_STICKER_PACK_NAME;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.orhanobut.hawk.Hawk;
import com.sangcomz.fishbun.FishBun;
import com.sangcomz.fishbun.adapter.image.impl.GlideAdapter;
import com.stickers.bank.BuildConfig;
import com.stickers.bank.R;
import com.stickers.bank.data.common.Constants;
import com.stickers.bank.data.listeners.InterstitialAdvListener;
import com.stickers.bank.databinding.ActivityCreateStickerBinding;
import com.stickers.bank.ui.base.BaseActivity;
import com.stickers.bank.ui.sticker.CutOut;
import com.stickers.bank.ui.sticker.StickerCustom;
import com.stickers.bank.ui.sticker.StickerPackCustom;
import com.stickers.bank.utils.AdvUtils;
import com.stickers.bank.utils.GridSpacingItemDecoration;
import com.stickers.bank.utils.PermissionUtils;
import com.stickers.bank.utils.Preferences;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CreateStickerActivity extends BaseActivity<ActivityCreateStickerBinding> implements View.OnClickListener {

    public static final int SELECT_GALLERY_IMAGE_CODE_TO_TRY_IMAGE = 1572;
    public static final int SELECT_GALLERY_IMAGE_CODE_TO_STICKER = 1578;
    public static final int EDITOR_IMAGE_CODE_TO_TRY_IMAGE = 1592;
    public static final int REMOVE_BG_IMAGE_CODE_TO_STICKER = 1568;
    public static final int REMOVE_BG__IMAGE_CODE_TO_TRY_IMAGE = 1562;
    public static final int EDITOR_IMAGE_CODE_TO_STICKER = 1598;

    private int PICK_IMAGE_TRAY_CIRCLE = 300;
    private int PICK_IMAGE_TRAY_RECTANGLE = 301;
    private int PICK_IMAGE_TRAY_NO_CROP = 302;

    private int PICK_IMAGE_STICKER_ADD_CIRCLE = 200;
    private int PICK_IMAGE_STICKER_ADD_RECTANGLE = 201;
    private int PICK_IMAGE_STICKER_ADD_NO_CROP = 202;

    private List<Bitmap> stickersList = new ArrayList<>();
    private BitmapListAdapter adapter;
    private Bitmap TrayImage;
    private String imageurl;

    List<StickerCustom> mStickers = new ArrayList<>();
    List<String> mEmojis, mDownloadFiles;
    private StickerPackCustom stickerPack;
    public static String mainpath;
    private static final int ADD_PACK = 22200;
    private String packId;

    Integer counter = 0;

    @Override
    public int getLayoutId() {
        return R.layout.activity_create_sticker;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        binding.header.btnBack.setVisibility(View.VISIBLE);
        binding.header.tvTitle.setText(getString(R.string.custom_sticker));
        //AdvUtils.getInstance(this).loadShowBMed(binding.flBanner, AdvUtils.getAdSize(CreateStickerActivity.this));
        mainpath = getFilesDir() + "/" + "stickers_asset";

        adapter = new BitmapListAdapter();
        int spanCount = 4; // 3 columns
        binding.rvStickers.setLayoutManager(new GridLayoutManager(CreateStickerActivity.this, spanCount));
        int spacing = 22; // 22px
        boolean includeEdge = true;
        binding.rvStickers.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
        binding.rvStickers.setAdapter(adapter);

        int id = (new Random().nextInt(99999) + 10000);
        packId = String.valueOf(id);
    }

    @Override
    protected void setListeners() {
        binding.header.btnBack.setOnClickListener(this);
        binding.btnExpand.setOnClickListener(this);
        binding.mcvAddTray.setOnClickListener(this);
        binding.btnAddSticker.setOnClickListener(this);
        binding.btnAddWa.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                onBackPressed();
                break;
            case R.id.btn_expand:
                if (binding.llOtherData.isShown()) {
                    binding.llOtherData.setVisibility(View.GONE);
                    binding.btnExpand.setIcon(getDrawable(R.drawable.ic_expand_more));
                } else {
                    binding.llOtherData.setVisibility(View.VISIBLE);
                    binding.btnExpand.setIcon(getDrawable(R.drawable.ic_expand_less));
                }
                break;
            case R.id.mcv_add_tray:
                if (PermissionUtils.checkReadWriteStoragePermission(CreateStickerActivity.this)) {
                    DialogTrayImage();
                } else {
                    PermissionUtils.requestPermission(CreateStickerActivity.this);
                }

                break;
            case R.id.btn_add_sticker:
                if (PermissionUtils.checkReadWriteStoragePermission(CreateStickerActivity.this)) {
                    DialogaddSticker();
                } else {
                    PermissionUtils.requestPermission(CreateStickerActivity.this);
                }
                break;
            case R.id.btn_add_wa:
                createPack();
                break;
        }
    }

    /*public boolean checkReadWriteStoragePermission() {
        int permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission1 != PackageManager.PERMISSION_GRANTED || permission2 != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    public void requestReadWriteStoragePermission(String[] permissionsReadWriteStorageCamera, int myPermissionReadWriteStorage) {
        ActivityCompat.requestPermissions(this, permissionsReadWriteStorageCamera, myPermissionReadWriteStorage);
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_WRITE_STORAGE:
                boolean isGrantedAll = false;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        isGrantedAll = true;
                    } else {
                        isGrantedAll = false;
                        break;
                    }
                }
                if (isGrantedAll) {
                    DialogTrayImage();
                } else {
                    showToastMsg(getString(R.string.plz_grant_storage_permission));
                }
                break;
        }
    }

    public void DialogTrayImage() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.WHITE));
        dialog.setContentView(R.layout.crop_mode_dialog);

        RelativeLayout relative_layout_crop_as_circle = (RelativeLayout) dialog.findViewById(R.id.relative_layout_crop_as_circle);
        RelativeLayout relative_layout_crop_as_rect = (RelativeLayout) dialog.findViewById(R.id.relative_layout_crop_as_rect);
        RelativeLayout relative_layout_no_crop = (RelativeLayout) dialog.findViewById(R.id.relative_layout_no_crop);
        relative_layout_no_crop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectTrayImage();
                dialog.dismiss();
            }
        });
        relative_layout_crop_as_circle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionUtils.checkReadWriteStoragePermission(CreateStickerActivity.this)) {
                    //openAblumWithPermissionsCheck(1003);
                    openAlbum(1003);

                } else {
                    PermissionUtils.requestPermission(CreateStickerActivity.this);
                }
                dialog.dismiss();
            }
        });
        relative_layout_crop_as_rect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionUtils.checkReadWriteStoragePermission(CreateStickerActivity.this)) {
                    //openAblumWithPermissionsCheck(1004);
                    openAlbum(1004);

                } else {
                    //requestReadWriteStoragePermission(PERMISSIONS_READ_WRITE_STORAGE, REQUEST_PERMISSION_READ_WRITE_STORAGE);
                    PermissionUtils.requestPermission(CreateStickerActivity.this);
                }
                dialog.dismiss();
            }
        });

        dialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                }
                return true;
            }
        });
        dialog.show();
    }

    public void SelectTrayImage() {
        openAlbum(SELECT_GALLERY_IMAGE_CODE_TO_TRY_IMAGE);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            openAblumWithPermissionsCheck(SELECT_GALLERY_IMAGE_CODE_TO_TRY_IMAGE);
        } else {
            openAlbum(SELECT_GALLERY_IMAGE_CODE_TO_TRY_IMAGE);
        }*/
    }

   /* private void openAblumWithPermissionsCheck(int code) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, code);
            return;
        }
        openAlbum(code);
    }*/

    private void openAlbum(int code) {
        FishBun.with(CreateStickerActivity.this)
                .setImageAdapter(new GlideAdapter())
                .setMaxCount(1)
                .exceptGif(true)
                .setMinCount(1)
                .setActionBarColor(Color.parseColor("#000000"), Color.parseColor("#000000"), false)
                .setActionBarTitleColor(Color.parseColor("#ffffff"))
                .setRequestCode(code)
                .startAlbum();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_GALLERY_IMAGE_CODE_TO_STICKER: {
                    ArrayList<Uri> dk = data.getParcelableArrayListExtra(Constants.INTENT_PATH);
                    Uri filepath = dk.get(0);
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), filepath);
                    } catch (IOException e) {
                    }
                    File file = getFileFromBitmap(bitmap);
                    EditImageActivity.start(this, file.getAbsolutePath(), file.getAbsolutePath(), EDITOR_IMAGE_CODE_TO_STICKER);
                    break;
                }
                case SELECT_GALLERY_IMAGE_CODE_TO_TRY_IMAGE: {
                    ArrayList<Uri> dk = data.getParcelableArrayListExtra(Constants.INTENT_PATH);
                    Uri filepath = dk.get(0);
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), filepath);
                    } catch (IOException e) {
                    }
                    File file = getFileFromBitmap(bitmap);
                    EditImageActivity.start(this, file.getAbsolutePath(), file.getAbsolutePath(), EDITOR_IMAGE_CODE_TO_TRY_IMAGE);
                    break;
                }
                case REMOVE_BG_IMAGE_CODE_TO_STICKER: {
                    String newFilePath = data.getStringExtra(CutOut.CUTOUT_EXTRA_RESULT);


                    File file = new File(newFilePath);

                    String filePath = file.getPath();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);

                    if (bitmap == null) {
                        Toast.makeText(this, "null", Toast.LENGTH_SHORT).show();
                    }

                    stickersList.add(bitmap);
                    adapter.notifyDataSetChanged();

                    break;
                }
                case REMOVE_BG__IMAGE_CODE_TO_TRY_IMAGE: {
                    String newFilePath = data.getStringExtra(CutOut.CUTOUT_EXTRA_RESULT);


                    File file = new File(newFilePath);

                    String filePath = file.getPath();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);

                    if (bitmap == null) {
                        Toast.makeText(this, "null", Toast.LENGTH_SHORT).show();
                    }

                    binding.ivTray.setImageBitmap(bitmap);
                    TrayImage = bitmap;
                    imageurl = filePath;
                    break;
                }
                case EDITOR_IMAGE_CODE_TO_STICKER: {
                    String newFilePath = data.getStringExtra(EditImageActivity.EXTRA_OUTPUT);

                    boolean isImageEdit = data.getBooleanExtra(EditImageActivity.IMAGE_IS_EDIT, false);

                    if (isImageEdit) {
                    } else {//未编辑  还是用原来的图片
                        newFilePath = data.getStringExtra(EditImageActivity.FILE_PATH);
                    }
                    Intent intent = new Intent(CreateStickerActivity.this, EditorActivity.class);
                    intent.putExtra("uri", newFilePath);
                    startActivityForResult(intent, REMOVE_BG_IMAGE_CODE_TO_STICKER);

                    break;
                }
                case EDITOR_IMAGE_CODE_TO_TRY_IMAGE: {
                    String newFilePath = data.getStringExtra(EditImageActivity.EXTRA_OUTPUT);

                    boolean isImageEdit = data.getBooleanExtra(EditImageActivity.IMAGE_IS_EDIT, false);

                    if (isImageEdit) {
                    } else {//未编辑  还是用原来的图片
                        newFilePath = data.getStringExtra(EditImageActivity.FILE_PATH);
                    }
                    Intent intent = new Intent(CreateStickerActivity.this, EditorActivity.class);
                    intent.putExtra("uri", newFilePath);
                    startActivityForResult(intent, REMOVE_BG__IMAGE_CODE_TO_TRY_IMAGE);
                    break;
                }
            }
        }

        if (requestCode == 1003) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<Uri> uries;
                uries = data.getParcelableArrayListExtra(Constants.INTENT_PATH);

                CropImage.ActivityBuilder cropImge = CropImage.activity(uries.get(0))
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setActivityTitle("Crop Your TryImage")
                        .setAllowFlipping(true)
                        .setFixAspectRatio(true)
                        .setScaleType(CropImageView.ScaleType.FIT_CENTER)
                        .setActivityMenuIconColor(R.color.black)
                        .setCropMenuCropButtonIcon(R.drawable.ic_check);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cropImge.setCropShape(CropImageView.CropShape.RECTANGLE);
                } else {
                    cropImge.setCropShape(CropImageView.CropShape.OVAL);
                }
                Intent intent = cropImge.getIntent(CreateStickerActivity.this);
                startActivityForResult(intent, PICK_IMAGE_TRAY_CIRCLE);
            }
        }
        if (requestCode == 1004) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<Uri> uries;
                uries = data.getParcelableArrayListExtra(Constants.INTENT_PATH);

                CropImage.ActivityBuilder cropImge = CropImage.activity(uries.get(0))
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setActivityTitle("Crop Your TryImage")
                        .setAllowFlipping(true)
                        .setFixAspectRatio(true)
                        .setScaleType(CropImageView.ScaleType.FIT_CENTER)
                        .setCropShape(CropImageView.CropShape.RECTANGLE)
                        .setActivityMenuIconColor(R.color.black)
                        .setCropMenuCropButtonIcon(R.drawable.ic_check);
                Intent intent = cropImge.getIntent(CreateStickerActivity.this);
                startActivityForResult(intent, PICK_IMAGE_TRAY_RECTANGLE);
            }
        }
        if (requestCode == 1005) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<Uri> uries;
                uries = data.getParcelableArrayListExtra(Constants.INTENT_PATH);

                CropImage.ActivityBuilder cropImge = CropImage.activity(uries.get(0))
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setActivityTitle("Crop Your TryImage")
                        .setAllowFlipping(true)
                        .setFixAspectRatio(true)
                        .setScaleType(CropImageView.ScaleType.FIT_CENTER)
                        .setActivityMenuIconColor(R.color.black)
                        .setCropMenuCropButtonIcon(R.drawable.ic_check);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cropImge.setCropShape(CropImageView.CropShape.RECTANGLE);
                } else {
                    cropImge.setCropShape(CropImageView.CropShape.OVAL);
                }
                Intent intent = cropImge.getIntent(CreateStickerActivity.this);
                startActivityForResult(intent, PICK_IMAGE_STICKER_ADD_CIRCLE);
            }
        }
        if (requestCode == 1006) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<Uri> uries;
                uries = data.getParcelableArrayListExtra(Constants.INTENT_PATH);

                CropImage.ActivityBuilder cropImge = CropImage.activity(uries.get(0))
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setActivityTitle("Crop Your TryImage")
                        .setAllowFlipping(true)
                        .setFixAspectRatio(true)
                        .setScaleType(CropImageView.ScaleType.FIT_CENTER)
                        .setActivityMenuIconColor(R.color.black)
                        .setCropMenuCropButtonIcon(R.drawable.ic_check);
                cropImge.setCropShape(CropImageView.CropShape.RECTANGLE);

                Intent intent = cropImge.getIntent(CreateStickerActivity.this);
                startActivityForResult(intent, PICK_IMAGE_STICKER_ADD_RECTANGLE);
            }
        }
        if (requestCode == CutOut.CUTOUT_ACTIVITY_REQUEST_CODE) {

            if (resultCode == RESULT_OK) {
                Uri selectedImage = CutOut.getUri(data);

                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                binding.ivTray.setImageBitmap(bitmap);
                TrayImage = bitmap;
                imageurl = selectedImage.getPath();
            }
        }
        if (requestCode == CutOut.CUTOUT_ACTIVITY_REQUEST_CODE_STICKER) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = CutOut.getUri(data);

                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (bitmap != null) {
                    stickersList.add(bitmap);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "null", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (requestCode == PICK_IMAGE_TRAY_RECTANGLE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                binding.ivTray.setImageBitmap(bitmap);
                TrayImage = bitmap;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        if (requestCode == PICK_IMAGE_TRAY_CIRCLE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                binding.ivTray.setImageBitmap(getCroppedBitmap(bitmap));
                TrayImage = getCroppedBitmap(bitmap);
                imageurl = resultUri.getPath();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        if (requestCode == PICK_IMAGE_STICKER_ADD_CIRCLE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                stickersList.add(getCroppedBitmap(bitmap));
                adapter.notifyDataSetChanged();

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        if (requestCode == PICK_IMAGE_STICKER_ADD_RECTANGLE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                stickersList.add(bitmap);
                adapter.notifyDataSetChanged();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        if (requestCode == PICK_IMAGE_STICKER_ADD_NO_CROP) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
                if (bitmap != null) {
                    stickersList.add(bitmap);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "null", Toast.LENGTH_SHORT).show();
                }

            }
        }
        if (requestCode == PICK_IMAGE_TRAY_NO_CROP) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
                if (bitmap != null) {
                    binding.ivTray.setImageBitmap(bitmap);
                    TrayImage = bitmap;
                    imageurl = picturePath;
                } else {
                    Toast.makeText(this, "null", Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (requestCode == ADD_PACK) {
            if (resultCode == Activity.RESULT_CANCELED) {
                if (data != null) {
                    final String validationError = data.getStringExtra("validation_error");
                    if (validationError != null) {
                        if (BuildConfig.DEBUG) {
                            //validation error should be shown to developer only, not users.
                            BankFolderDetailsActivity.MessageDialogFragment.newInstance(R.string.title_validation_error, validationError).show(getSupportFragmentManager(), "validation error");
                        }
                        Log.e("AddStickerPackActivity", "Validation failed:" + validationError);
                    }
                } else {
                    new BankFolderDetailsActivity.StickerPackNotAddedMessageFragment().show(getSupportFragmentManager(), "sticker_pack_not_added");
                }
            } else if (resultCode == Activity.RESULT_OK) {
                int version = Preferences.getInt(Preferences.KEY_VERSION);
                Preferences.setInt(Preferences.KEY_VERSION, ++version);

                new MaterialAlertDialogBuilder(CreateStickerActivity.this, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                        .setMessage("Sticker pack updated.")
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                            AdvUtils.getInstance(CreateStickerActivity.this).showInterstitialAlternate(new InterstitialAdvListener() {
                                @Override
                                public void onInterstitialAdLoaded() {

                                }

                                @Override
                                public void onInterstitialAdClosed() {
                                    finish();
                                }

                                @Override
                                public void onContinue() {
                                    finish();
                                }
                            });
                        }).show();
            }
        }
    }

    public File getFileFromBitmap(Bitmap resource) {

        int width = 512; // - Dimension in pixels
        int height = 512;  // - Dimension in pixels
        if (resource.getWidth() != resource.getHeight()) {
            resource = getResizedBitmap(resource);
        }
        Bitmap bitmap = Bitmap.createScaledBitmap(
                resource, width, height, false);
        counter++;
        OutputStream fOut = null;
        File file = new File(getApplicationContext().getCacheDir(), "Sticker_" + counter + ".png");
        try {
            fOut = new FileOutputStream(file);
            Bitmap pictureBitmap = bitmap; // obtaining the Bitmap
            pictureBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            fOut.flush(); // Not really required
            fOut.close(); // do not forget to close the stream

            //MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public Bitmap getResizedBitmap(Bitmap srcBmp) {
        Bitmap dstBmp;
        if (srcBmp.getWidth() >= srcBmp.getHeight()) {

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth() / 2 - srcBmp.getHeight() / 2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );

        } else {

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight() / 2 - srcBmp.getWidth() / 2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }
        return dstBmp;
    }

    public class BitmapListAdapter extends RecyclerView.Adapter<BitmapListAdapter.Holder> {

        @NonNull
        @Override
        public BitmapListAdapter.Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_bitmap, null);
            BitmapListAdapter.Holder mh = new BitmapListAdapter.Holder(v);
            return mh;
        }

        @Override
        public void onBindViewHolder(@NonNull BitmapListAdapter.Holder holder, @SuppressLint("RecyclerView") final int position) {
            holder.bitmap_image.setImageBitmap(stickersList.get(position));
            holder.button_bitmap_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stickersList.remove(position);
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return stickersList.size();
        }

        public class Holder extends RecyclerView.ViewHolder {

            private final ImageView bitmap_image;
            private final Button button_bitmap_item;

            public Holder(@NonNull View itemView) {
                super(itemView);
                this.bitmap_image = (ImageView) itemView.findViewById(R.id.bitmap_image);
                this.button_bitmap_item = (Button) itemView.findViewById(R.id.button_bitmap_item);
            }
        }
    }

    public void DialogaddSticker() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.WHITE));
        dialog.setContentView(R.layout.crop_mode_dialog);

        RelativeLayout relative_layout_crop_as_circle = (RelativeLayout) dialog.findViewById(R.id.relative_layout_crop_as_circle);
        RelativeLayout relative_layout_crop_as_rect = (RelativeLayout) dialog.findViewById(R.id.relative_layout_crop_as_rect);
        RelativeLayout relative_layout_no_crop = (RelativeLayout) dialog.findViewById(R.id.relative_layout_no_crop);
        relative_layout_no_crop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSticker();
                dialog.dismiss();

            }
        });
        relative_layout_crop_as_circle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //openAblumWithPermissionsCheck(1005);

                dialog.dismiss();
            }
        });
        relative_layout_crop_as_rect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //openAblumWithPermissionsCheck(1006);
                openAlbum(1006);

                dialog.dismiss();
            }
        });

        dialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                }
                return true;
            }
        });
        dialog.show();
    }

    public void addSticker() {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            openAblumWithPermissionsCheck(SELECT_GALLERY_IMAGE_CODE_TO_STICKER);
        } else {
            openAblum(SELECT_GALLERY_IMAGE_CODE_TO_STICKER);
        }*/
        openAlbum(SELECT_GALLERY_IMAGE_CODE_TO_STICKER);

    }

    private void openAblum(int code) {
        FishBun.with(CreateStickerActivity.this)
                .setImageAdapter(new GlideAdapter())
                .setMaxCount(1)
                .exceptGif(true)
                .setMinCount(1)
                .setActionBarColor(Color.parseColor("#25D366"), Color.parseColor("#25D366"), false)
                .setActionBarTitleColor(Color.parseColor("#ffffff"))
                .setRequestCode(code)
                .startAlbum();
    }

    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }

    public void createPack() {
        if (binding.edtPackName.getText().toString().trim().length() < 4) {
            showToastMsg(getResources().getString(R.string.name_short));
            return;
        }
        if (binding.edtPublisher.getText().toString().trim().length() < 3) {
            showToastMsg(getResources().getString(R.string.publisher_short));
            return;
        }
        if (TrayImage == null) {
            showToastMsg(getResources().getString(R.string.tray_image_required));
            return;
        }
        if (stickersList.size() < 3) {
            showToastMsg(getResources().getString(R.string.sticker_number_required));
            return;
        }
        if (stickersList.size() > 30) {
            showToastMsg(getResources().getString(R.string.sticker_number_required_min));
            return;
        }
        mStickers = new ArrayList<>();
        mEmojis = new ArrayList<>();
        mDownloadFiles = new ArrayList<>();
        mEmojis.add("");

        stickerPack = new StickerPackCustom(
                packId,
                binding.edtPackName.getText().toString().trim(),
                binding.edtPublisher.getText().toString().trim(),
                "tray_image.png",
                "tray_image.png",
                "0",
                "0",
                "false",
                "false",
                "now",
                "1",
                "none",
                "me",
                binding.edtEmail.getText().toString().trim(),
                binding.edtWebsite.getText().toString().trim(),
                "",
                "",
                false
        );
        for (int j = 0; j < stickersList.size(); j++) {
            mStickers.add(new StickerCustom(
                    "sticker_" + j + ".webp",
                    "sticker_" + j + ".webp",
                    "sticker_" + j + ".webp",
                    mEmojis
            ));
            mDownloadFiles.add("sticker_" + j + ".webp");
        }
        Hawk.put(stickerPack.identifier, mStickers);
        stickerPack.setStickers(Hawk.get(stickerPack.identifier, new ArrayList<StickerCustom>()));

        new DownloadFileFromURL().execute();
    }

    public void showToastMsg(String msg) {
        Toast.makeText(CreateStickerActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {
        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // linear_layout_add_to_whatsapp.setVisibility(View.GONE);
            // linear_layout_progress.setVisibility(View.VISIBLE);
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String... f_url) {

            try {

                int width_try = 96; // - Dimension in pixels
                int height_try = 96;  // - Dimension in pixels
                Bitmap bitmap_try = Bitmap.createScaledBitmap(TrayImage, width_try, height_try, false);

                SaveTryImage(bitmap_try, "tray_image.png", stickerPack.identifier);

                ArrayList<StickerPackCustom> stickerPacks = Hawk.get("whatsapp_sticker_packs", new ArrayList<StickerPackCustom>());
                if (stickerPacks == null) {
                    stickerPacks = new ArrayList<>();
                } else {

                }
                for (int i = 0; i < stickerPacks.size(); i++) {
                    if (stickerPacks.get(i).identifier == packId) {

                        stickerPacks.remove(i);
                        i--;
                    }
                }
                stickerPacks.add(stickerPack);
                Hawk.put("whatsapp_sticker_packs", stickerPacks);

                int progress = 0;
                for (final StickerCustom s : stickerPack.getStickers()) {

                    Bitmap resource = stickersList.get(progress);

                    int width = 512; // - Dimension in pixels
                    int height = 512;  // - Dimension in pixels
                    if (resource.getWidth() != resource.getHeight()) {
                        resource = getResizedBitmap(resource);
                    }
                    Bitmap bitmap1 = Bitmap.createScaledBitmap(resource, width, height, false);

                    SaveImage(bitmap1, s.imageFileName, stickerPack.identifier);
                    progress++;
                    publishProgress("" + (int) ((progress * 100) / stickerPack.getStickers().size()));
                }


            } catch (Exception e) {
                Log.e("PACKSTICKER", e.getMessage());

            }
            return null;
        }

        /**
         * Updating progress bar
         */
        protected void onProgressUpdate(String... progress) {
            //progress_bar_pack.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         **/
        @Override
        protected void onPostExecute(String file_url) {
            // linear_layout_add_to_whatsapp.setVisibility(View.VISIBLE);
            // linear_layout_progress.setVisibility(View.GONE);
            Addtowhatsapp();
        }
    }

    public static void SaveTryImage(Bitmap finalBitmap, String name, String identifier) {

        String root = mainpath + "/" + identifier;
        File myDir = new File(root + "/" + "try");
        myDir.mkdirs();
        String fname = name.replace(".png", "").replace(" ", "_") + ".png";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 40, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void SaveImage(Bitmap finalBitmap, String name, String identifier) {

        String root = mainpath + "/" + identifier;
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = name;
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.WEBP, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
            Log.v("error when save", e.getMessage());
        }
    }

    public void Addtowhatsapp() {
        Intent intent = new Intent();
        intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        intent.putExtra(EXTRA_STICKER_PACK_ID, packId);
        intent.putExtra(EXTRA_STICKER_PACK_AUTHORITY, BuildConfig.CONTENT_PROVIDER_AUTHORITY);
        intent.putExtra(EXTRA_STICKER_PACK_NAME, binding.edtPackName.getText().toString().trim());
        try {
            startActivityForResult(intent, ADD_PACK);
        } catch (ActivityNotFoundException e) {
            showToastMsg("WhatsApp Application not installed on this device");
        }
    }
}