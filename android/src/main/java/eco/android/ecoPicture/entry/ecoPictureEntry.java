package eco.android.ecoPicture.entry;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.luck.picture.lib.PictureSelectionModel;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.broadcast.BroadcastAction;
import com.luck.picture.lib.broadcast.BroadcastManager;
import com.luck.picture.lib.compress.Luban;
import com.luck.picture.lib.compress.OnCompressListener;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.style.PictureCropParameterStyle;
import com.luck.picture.lib.style.PictureParameterStyle;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.eco.framework.activity.PageActivity;
import app.eco.framework.extend.annotation.ModuleEntry;
import app.eco.framework.extend.bean.WebCallBean;
import app.eco.framework.extend.module.ecoCommon;
import app.eco.framework.extend.module.ecoJson;
import app.eco.framework.extend.module.ecoMap;
import app.eco.framework.extend.module.ecoParse;
import eco.android.ecoPicture.R;
import eco.android.ecoPicture.engine.GlideEngine;
import eco.android.ecoPicture.module.ecoPictureWebModule;
import eco.android.ecoPicture.module.ecoPictureAppModule;

@ModuleEntry
public class ecoPictureEntry {

    /**
     * APP启动会运行此函数方法
     * @param content Application
     */
    public void init(Context content) {

        //1、注册weex模块
        try {
            WXSDKEngine.registerModule("ecoPicture", ecoPictureAppModule.class);
        } catch (WXException e) {
            e.printStackTrace();
        }

        //2、注册web模块（web-view模块可通过requireModuleJs调用，调用详见：https://eco.app/component/web-view.html）
        WebCallBean.addClassData("ecoPicture", ecoPictureWebModule.class);
    }

    /****************************************************************************************/
    /****************************************************************************************/
    /****************************************************************************************/

    private List<LocalMedia> mLocalMediaLists = new ArrayList<>();;

    private PictureParameterStyle mPictureParameterStyle;

    private PictureCropParameterStyle mCropParameterStyle;

    private boolean isBroadcast = false;

    private JSCallback broadcastCallback = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            Bundle extras;
            switch (action) {
                case BroadcastAction.ACTION_DELETE_PREVIEW_POSITION:
                    // 外部预览删除按钮回调
                    extras = intent.getExtras();
                    if (extras == null) {
                        return;
                    }
                    int position = extras.getInt(PictureConfig.EXTRA_PREVIEW_DELETE_POSITION);
                    if (broadcastCallback != null) {
                        JSONObject callData = new JSONObject();
                        callData.put("position", position);
                        broadcastCallback.invokeAndKeepAlive(callData);
                    }
                    break;
            }
        }
    };

    private List<LocalMedia> toLocalMedia(JSONArray selectedList) {
        List<LocalMedia> selected = new ArrayList<>();
        if (selectedList != null) {
            for (int i = 0; i <  selectedList.size(); i++) {
                String path = ecoJson.getString(ecoJson.parseObject(selectedList.get(i)), "path");
                for (int j = 0; j <  mLocalMediaLists.size(); j++) {
                    LocalMedia tempMedia = mLocalMediaLists.get(j);
                    if (tempMedia != null) {
                        if (path.contentEquals(getPath(tempMedia))) {
                            selected.add(tempMedia);
                        }
                    }
                }
            }
        }
        return selected;
    }

    private JSONArray toJSONArray(List<LocalMedia> result) {
        JSONArray lists = new JSONArray();
        for (int i = 0; i <  result.size(); i++) {
            LocalMedia media = result.get(i);
            JSONObject tmpObj = new JSONObject();
            tmpObj.put("path", getPath(media));
            tmpObj.put("cutPath", ecoParse.parseStr(media.getCutPath()));
            tmpObj.put("compressPath", ecoParse.parseStr(media.getCompressPath()));
            tmpObj.put("isCut", media.isCut());
            tmpObj.put("isCompressed", media.isCompressed());
            tmpObj.put("compressed", media.isCompressed());     //废弃
            tmpObj.put("mimeType", media.getMimeType());
            lists.add(tmpObj);
        }
        return lists;
    }

    private String getPath(LocalMedia media) {
        String path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            path = media.getAndroidQToPath();
        } else {
            path = media.getPath();
        }
        if (TextUtils.isEmpty(path)) {
            path = media.getRealPath();
        }
        return ecoParse.parseStr(path);
    }

    private void getWeChatStyle(Context context) {
        // 相册主题
        mPictureParameterStyle = new PictureParameterStyle();
        // 是否改变状态栏字体颜色(黑白切换)
        mPictureParameterStyle.isChangeStatusBarFontColor = false;
        // 是否开启右下角已完成(0/9)风格
        mPictureParameterStyle.isOpenCompletedNumStyle = false;
        // 是否开启类似QQ相册带数字选择风格
        mPictureParameterStyle.isOpenCheckNumStyle = true;
        // 状态栏背景色
        mPictureParameterStyle.pictureStatusBarColor = Color.parseColor("#393a3e");
        // 相册列表标题栏背景色
        mPictureParameterStyle.pictureTitleBarBackgroundColor = Color.parseColor("#393a3e");
        // 相册父容器背景色
        mPictureParameterStyle.pictureContainerBackgroundColor = ContextCompat.getColor(context, R.color.app_color_black);
        // 相册列表标题栏右侧上拉箭头
        mPictureParameterStyle.pictureTitleUpResId = R.drawable.picture_icon_wechat_up;
        // 相册列表标题栏右侧下拉箭头
        mPictureParameterStyle.pictureTitleDownResId = R.drawable.picture_icon_wechat_down;
        // 相册文件夹列表选中圆点
        mPictureParameterStyle.pictureFolderCheckedDotStyle = R.drawable.picture_orange_oval;
        // 相册返回箭头
        mPictureParameterStyle.pictureLeftBackIcon = R.drawable.picture_icon_close;
        // 标题栏字体颜色
        mPictureParameterStyle.pictureTitleTextColor = ContextCompat.getColor(context, R.color.picture_color_white);
        // 相册右侧按钮字体颜色  废弃 改用.pictureRightDefaultTextColor和.pictureRightDefaultTextColor
        mPictureParameterStyle.pictureCancelTextColor = ContextCompat.getColor(context, R.color.picture_color_53575e);
        // 相册右侧按钮字体默认颜色
        mPictureParameterStyle.pictureRightDefaultTextColor = ContextCompat.getColor(context, R.color.picture_color_53575e);
        // 相册右侧按可点击字体颜色,只针对isWeChatStyle 为true时有效果
        mPictureParameterStyle.pictureRightSelectedTextColor = ContextCompat.getColor(context, R.color.picture_color_white);
        // 相册右侧按钮背景样式,只针对isWeChatStyle 为true时有效果
        mPictureParameterStyle.pictureUnCompleteBackgroundStyle = R.drawable.picture_send_button_default_bg;
        // 相册右侧按钮可点击背景样式,只针对isWeChatStyle 为true时有效果
        mPictureParameterStyle.pictureCompleteBackgroundStyle = R.drawable.picture_send_button_bg;
        // 相册列表勾选图片样式
        mPictureParameterStyle.pictureCheckedStyle = R.drawable.picture_wechat_num_selector;
        // 相册标题背景样式 ,只针对isWeChatStyle 为true时有效果
        mPictureParameterStyle.pictureWeChatTitleBackgroundStyle = R.drawable.picture_album_bg;
        // 微信样式 预览右下角样式 ,只针对isWeChatStyle 为true时有效果
        mPictureParameterStyle.pictureWeChatChooseStyle = R.drawable.picture_wechat_select_cb;
        // 相册返回箭头 ,只针对isWeChatStyle 为true时有效果
        mPictureParameterStyle.pictureWeChatLeftBackStyle = R.drawable.picture_icon_back;
        // 相册列表底部背景色
        mPictureParameterStyle.pictureBottomBgColor = ContextCompat.getColor(context, R.color.picture_color_grey);
        // 已选数量圆点背景样式
        mPictureParameterStyle.pictureCheckNumBgStyle = R.drawable.picture_num_oval;
        // 相册列表底下预览文字色值(预览按钮可点击时的色值)
        mPictureParameterStyle.picturePreviewTextColor = ContextCompat.getColor(context, R.color.picture_color_white);
        // 相册列表底下不可预览文字色值(预览按钮不可点击时的色值)
        mPictureParameterStyle.pictureUnPreviewTextColor = ContextCompat.getColor(context, R.color.picture_color_9b);
        // 相册列表已完成色值(已完成 可点击色值)
        mPictureParameterStyle.pictureCompleteTextColor = ContextCompat.getColor(context, R.color.picture_color_white);
        // 相册列表未完成色值(请选择 不可点击色值)
        mPictureParameterStyle.pictureUnCompleteTextColor = ContextCompat.getColor(context, R.color.picture_color_53575e);
        // 预览界面底部背景色
        mPictureParameterStyle.picturePreviewBottomBgColor = ContextCompat.getColor(context, R.color.picture_color_half_grey);
        // 外部预览界面删除按钮样式
        mPictureParameterStyle.pictureExternalPreviewDeleteStyle = R.drawable.picture_icon_delete;
        // 原图按钮勾选样式  需设置.isOriginalImageControl(true); 才有效
        mPictureParameterStyle.pictureOriginalControlStyle = R.drawable.picture_original_wechat_checkbox;
        // 原图文字颜色 需设置.isOriginalImageControl(true); 才有效
        mPictureParameterStyle.pictureOriginalFontColor = ContextCompat.getColor(context, R.color.app_color_white);
        // 外部预览界面是否显示删除按钮
        mPictureParameterStyle.pictureExternalPreviewGonePreviewDelete = true;
        // 设置NavBar Color SDK Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP有效
        mPictureParameterStyle.pictureNavBarColor = Color.parseColor("#393a3e");

        // 裁剪主题
        mCropParameterStyle = new PictureCropParameterStyle(
                ContextCompat.getColor(context, R.color.app_color_grey),
                ContextCompat.getColor(context, R.color.app_color_grey),
                Color.parseColor("#393a3e"),
                ContextCompat.getColor(context, R.color.app_color_white),
                mPictureParameterStyle.isChangeStatusBarFontColor);
    }

    /***************************************************************************************************/
    /***************************************************************************************************/
    /***************************************************************************************************/

    /**
     * 打开相册
     * @param object
     * @param callback
     */
    public void create(Context context, String object, final JSCallback callback) {
        JSONObject json = ecoJson.parseObject(object);
        String pageName = ((PageActivity) context).getPageInfo().getPageName();
        //
        Map<String, Object> callData = new HashMap<>();
        callData.put("pageName", pageName);
        callData.put("status", "create");
        callback.invokeAndKeepAlive(callData);
        //
        getWeChatStyle(context);
        //
        List<LocalMedia> selected = toLocalMedia(ecoJson.parseArray(json.getString("selected")));
        PictureSelectionModel model;
        if (ecoJson.getString(json, "type", "gallery").equals("camera")) {
            model = PictureSelector
                    .create((Activity) context)
                    .openCamera(ecoJson.getInt(json, "gallery", PictureMimeType.ofAll())); // 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
        } else {
            model = PictureSelector
                    .create((Activity) context)
                    .openGallery(ecoJson.getInt(json, "gallery", PictureMimeType.ofAll())); // 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
        }
        GlideEngine mGlideEngine = GlideEngine.createGlideEngine();
        mGlideEngine.sizeMultiplier(ecoJson.getFloat(json, "multiplier", 0.5f));      // glide 加载图片大小 0~1之间 如设置
        mGlideEngine.glideOverride(ecoJson.getInt(json, "overrideWidth", 180), ecoJson.getInt(json, "overrideHeight", 180));      // int glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
        model.loadImageEngine(mGlideEngine)
                .isWeChatStyle(true)                                        // 是否开启微信图片选择风格
                .setPictureStyle(mPictureParameterStyle)                    // 动态自定义相册主题
                .setPictureCropStyle(mCropParameterStyle)                   // 动态自定义裁剪主题
                .maxSelectNum(ecoJson.getInt(json, "maxNum", 9))                      // 最大选择数量 int
                .minSelectNum(ecoJson.getInt(json, "minNum", 0))                      // 最小选择数量 int
                .imageSpanCount(ecoJson.getInt(json, "spanCount", 4))                 // 每行显示个数 int
                .selectionMode(ecoJson.getInt(json, "mode", PictureConfig.MULTIPLE))  // 多选 or 单选 PictureConfig.MULTIPLE or PictureConfig.SINGLE
                .previewImage(ecoJson.getBoolean(json, "previewImage", true))         // 是否可预览图片 true or false
                .previewVideo(ecoJson.getBoolean(json, "previewVideo", true))         // 是否可预览视频 true or false
                .enablePreviewAudio(ecoJson.getBoolean(json, "previewAudio", true))   // 是否可播放音频 true or false
                .isCamera(ecoJson.getBoolean(json, "camera", true))                   // 是否显示拍照按钮 true or false
                .imageFormat(ecoJson.getString(json, "format", PictureMimeType.JPEG)) // 拍照保存图片格式后缀,默认jpeg
                .isZoomAnim(ecoJson.getBoolean(json, "zoomAnim", true))               // 图片列表点击 缩放效果 默认true
                .enableCrop(ecoJson.getBoolean(json, "crop", false))                  // 是否裁剪 true or false
                .compress(ecoJson.getBoolean(json, "compress", false))                // 是否压缩 true or false
                .withAspectRatio(ecoJson.getInt(json, "ratioX", 1), ecoJson.getInt(json, "ratioY", 1))                      // int 裁剪比例 如16:9 3:2 3:4 1:1 可自定义
                .hideBottomControls(ecoJson.getBoolean(json, "cropControls", false))  // 是否显示uCrop工具栏，默认不显示 true or false
                .isGif(ecoJson.getBoolean(json, "gif", false))                        // 是否显示gif图片 true or false
                .freeStyleCropEnabled(ecoJson.getBoolean(json, "freeCrop", false))    // 裁剪框是否可拖拽 true or false
                .circleDimmedLayer(ecoJson.getBoolean(json, "circle", false))         // 是否圆形裁剪 true or false
                .showCropFrame(ecoJson.getBoolean(json, "cropFrame", true))           // 是否显示裁剪矩形边框 圆形裁剪时建议设为false   true or false
                .showCropGrid(ecoJson.getBoolean(json, "cropGrid", true))             // 是否显示裁剪矩形网格 圆形裁剪时建议设为false    true or false
                .openClickSound(ecoJson.getBoolean(json, "clickSound", false))        // 是否开启点击声音 true or false
                .selectionMedia(selected)                                              // 是否传入已选图片 List<LocalMedia> list
                .previewEggs(ecoJson.getBoolean(json, "eggs", false))                 // 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中) true or false
                .cutOutQuality(ecoJson.getInt(json, "quality", 90))                   // 裁剪压缩质量 默认90 int
                .minimumCompressSize(ecoJson.getInt(json, "compressSize", 100))       // 小于100kb的图片不压缩
                .synOrAsy(ecoJson.getBoolean(json, "sync", true))                     // 同步true或异步false 压缩 默认同步
                .cropImageWideHigh(ecoJson.getInt(json, "cropWidth", 0), ecoJson.getInt(json, "cropHeight", 0))   // 裁剪宽高比，设置如果大于图片本身宽高则无效 int
                .rotateEnabled(ecoJson.getBoolean(json, "rotate", true))              // 裁剪是否可旋转图片 true or false
                .scaleEnabled(ecoJson.getBoolean(json, "scale", true))                // 裁剪是否可放大缩小图片 true or false
                .videoQuality(ecoJson.getInt(json, "videoQuality", 0))                // 视频录制质量 0 or 1 int
                .videoMaxSecond(ecoJson.getInt(json, "videoMaxSecond", 15))           // 显示多少秒以内的视频or音频也可适用 int
                .videoMinSecond(ecoJson.getInt(json, "videoMinSecond", 10))           // 显示多少秒以内的视频or音频也可适用 int
                .recordVideoSecond(ecoJson.getInt(json, "recordVideoSecond", 60))     // 视频秒数录制 默认60s int
                .forResult(result -> {
                    mLocalMediaLists = result;
                    Map<String, Object> callData1 = new HashMap<>();
                    callData1.put("status", "success");
                    callData1.put("lists", toJSONArray(result));
                    callback.invokeAndKeepAlive(callData1);
                    //
                    callData1 = new HashMap<>();
                    callData1.put("pageName", pageName);
                    callData1.put("status", "destroy");
                    callback.invoke(callData1);
                });
    }

    /**
     * 压缩图片
     * @param object
     * @param callback
     */
    public void compressImage(Context context, String object, final JSCallback callback) {
        JSONObject json = ecoJson.parseObject(object);
        JSONArray lists = ecoJson.parseArray(json.getString("lists"));
        if (lists.size() == 0 && ecoJson.parseArray(object).size() > 0) {
            lists = ecoJson.parseArray(object);
        }
        //
        List<LocalMedia> selected = new ArrayList<>();
        for (Object src : lists) {
            LocalMedia tmpMedia = new LocalMedia();
            if (src instanceof String) {
                tmpMedia.setPath(ecoParse.parseStr(src));
                selected.add(tmpMedia);
            } else {
                JSONObject tmpObj = ecoJson.parseObject(src);
                if (!TextUtils.isEmpty(tmpObj.getString("path"))) {
                    tmpMedia.setPath(tmpObj.getString("path"));
                    selected.add(tmpMedia);
                }
            }
        }
        //
        if (selected.size() == 0) {
            Map<String, Object> callData = new HashMap<>();
            callData.put("status", "error");
            callData.put("lists", lists);
            callback.invokeAndKeepAlive(callData);
        }
        JSONArray finalLists = lists;
        Luban.with(context)
                .loadMediaData(selected)
                .setCompressQuality(ecoJson.getInt(json, "compressSize", 90))
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess(List<LocalMedia> list) {
                        if (callback != null) {
                            Map<String, Object> callData = new HashMap<>();
                            callData.put("status", "success");
                            callData.put("lists", toJSONArray(list));
                            callback.invokeAndKeepAlive(callData);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (callback != null) {
                            Map<String, Object> callData = new HashMap<>();
                            callData.put("status", "error");
                            callData.put("lists", finalLists);
                            callback.invokeAndKeepAlive(callData);
                        }
                    }
                }).launch();
    }

    /**
     * 预览图片
     * @param position
     * @param array
     */
    public void picturePreview(Context context, int position, String array, JSCallback callback) {
        JSONArray lists = ecoJson.parseArray(array);
        if (lists.size() == 0) {
            JSONObject tempJson = new JSONObject();
            tempJson.put("path", array);
            lists.add(tempJson);
        }
        //
        List<LocalMedia> selected = new ArrayList<>();
        for (Object src : lists) {
            LocalMedia tmpMedia = new LocalMedia();
            if (src instanceof String) {
                tmpMedia.setPath(ecoParse.parseStr(src));
                selected.add(tmpMedia);
            } else {
                JSONObject tmpObj = ecoJson.parseObject(src);
                if (!TextUtils.isEmpty(tmpObj.getString("path"))) {
                    tmpMedia.setPath(tmpObj.getString("path"));
                    selected.add(tmpMedia);
                }
            }
        }
        if (selected.size() == 0) {
            return;
        }
        //
        getWeChatStyle(context);
        mPictureParameterStyle.pictureExternalPreviewGonePreviewDelete = callback != null;
        if (!isBroadcast) {
            isBroadcast = true;
            broadcastCallback = callback;
            BroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, BroadcastAction.ACTION_DELETE_PREVIEW_POSITION);
            if (context instanceof PageActivity) {
                ((PageActivity) context).setPageStatusListener("__ecoPicture::" + ecoCommon.randomString(6), new JSCallback() {
                    @Override
                    public void invoke(Object data) {
                        Map<String, Object> retData = ecoMap.objectToMap(data);
                        if (retData == null) {
                            return;
                        }
                        String status = ecoParse.parseStr(retData.get("status"));
                        if ("destroy".equals(status)) {
                            BroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver, BroadcastAction.ACTION_DELETE_PREVIEW_POSITION);
                        }
                    }

                    @Override
                    public void invokeAndKeepAlive(Object data) {

                    }
                });
            }
        }
        //
        PictureSelector.create((Activity) context)
                .themeStyle(R.style.picture_default_style)
                .setPictureStyle(mPictureParameterStyle)// 动态自定义相册主题
                .isNotPreviewDownload(true)
                .loadImageEngine(GlideEngine.createGlideEngine())
                .openExternalPreview(position, selected);
    }

    /**
     * 预览视频
     * @param path
     */
    public void videoPreview(Context context, String path) {
        PictureSelector.create((Activity) context).externalPictureVideo(path);
    }

    /**
     * 缓存清除，包括裁剪和压缩后的缓存，要在上传成功后调用，注意：需要系统sd卡权限
     */
    public void deleteCache(Context context) {
        PictureFileUtils.deleteAllCacheDirFile(context);
    }
}
