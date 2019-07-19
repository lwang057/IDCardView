package com.lwang.idcardview;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.itep.device.bean.IDCard;
import com.itep.device.bean.RFIDcard;
import com.itep.device.idCard.IDCardInterface;
import com.itep.device.rfid.RFIDInterface;
import com.itep.device.system.SystemInterface;
import com.itep.device.util.HexDump;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private LoadingWebView mLoadingView;
    private RFIDInterface rfidInterface;
    private IDCardInterface iDCardInterface;
    private IdCardInfo idCardInfo;
    private SystemInterface systemInterface;
    private static final int TIMEOUT = 5;
    private String[] nation = {"汉", "蒙古", "回", "藏", "维吾尔", "苗", "彝", "壮", "布依", "朝鲜",
            "满", "侗", "瑶", "白", "土家", "哈尼", "哈萨克", "傣", "黎", "傈僳",
            "佤", "畲", "高山", "拉祜", "水", "东乡", "纳西", "景颇", "克尔克孜", "土",
            "达斡尔", "仫佬", "羌", "布朗", "撒拉", "毛南", "仡佬", "锡伯", "阿昌", "普米",
            "塔吉克", "怒", "乌兹别克", "俄罗斯", "鄂温克", "德昂", "保安", "裕固", "京", "塔塔尔",
            "独龙", "鄂伦春", "赫哲", "门巴", "珞巴", "基诺"
    };
    private String idCardUUID = "";
    private String readATR = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLoadingView = (LoadingWebView) findViewById(R.id.loadingView);

        rfidInterface = new RFIDInterface();
        iDCardInterface = new IDCardInterface();
        idCardInfo = new IdCardInfo();
        systemInterface = new SystemInterface();

        mLoadingView.loadMessageUrl("http://www.anyulock.com/anyuidls/public/login4App");
        mLoadingView.addProgressBar();
        mLoadingView.setOnIDCardClick(new LoadingWebView.OnIDCardClick() {
            @Override
            public void setClick() {
                iDCardRead();
            }
        });

        mLoadingView.setOnICCardClick(new LoadingWebView.OnICCardClick() {
            @Override
            public void setClick() {
                iCCardRead();
            }
        });
    }

    private void iDCardRead() {

        // 打开设备
        int openResult = iDCardInterface.open();
        if (0 != openResult) {
            readIDCardFail("确认读卡器已接好！");
            return;
        }

        // 寻卡
        IDCard detectResult = iDCardInterface.detect(TIMEOUT);
        if (0 != detectResult.getErrCode() || !detectResult.isFlag()) {
            readIDCardFail("请确认身份证放置正确！");
            iDCardInterface.close();
            return;
        }

        // 激活
        IDCard activeResult = iDCardInterface.activeCard(TIMEOUT);
        if (0 != activeResult.getErrCode() || !activeResult.isFlag()) {
            readIDCardFail("请确认身份证放置正确！");
            iDCardInterface.close();
            return;
        }

        // 读取
        IDCard readResult = iDCardInterface.readCard(TIMEOUT);
        if (0 != readResult.getErrCode() || !readResult.isFlag()) {
            readIDCardFail("请确认身份证放置正确！");
            iDCardInterface.close();
            return;
        }

        // 设置蜂鸣器
        systemInterface.beep(3, 0, TIMEOUT);

        // uuid
        byte[] cmd = {0x00, 0x36, 0x00, 0x00, 0x08};
        IDCard apduResult = iDCardInterface.APDUComm(cmd, TIMEOUT);
        if (apduResult.isFlag()) {
            idCardUUID = HexDump.toHexString(apduResult.getResData());
        }

        try {
            onReadIDData(readResult.getResData());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Map<String, String> map = new HashMap<>();
                    map.put("name", idCardInfo.getName());
                    map.put("sex", idCardInfo.getSex());
                    map.put("address", idCardInfo.getAddress());
                    map.put("birth", idCardInfo.getBirth());
                    map.put("id_num", idCardInfo.getIdNum());
                    map.put("sign_office", idCardInfo.getSignOffice());
                    map.put("start_date", idCardInfo.getUsefulStartDate());
                    map.put("end_date", idCardInfo.getUsefulEndDate());
                    map.put("uuid", idCardUUID);
                    Object data = JSONObject.toJSON(map);
                    mLoadingView.setIDCardData(data);
                }
            });

            iDCardInterface.close();
        } catch (Exception e) {
            iDCardInterface.close();
            e.printStackTrace();
        }
    }

    /**
     * 获取数据
     *
     * @param data
     * @return
     * @throws UnsupportedEncodingException
     */
    private IdCardInfo onReadIDData(byte[] data) throws UnsupportedEncodingException {

        int i = 0;
        int j = 0;
        int iOffset = 0;
        byte bysName[] = new byte[30];
        byte bysSexCode[] = new byte[2];
        byte bysNationCode[] = new byte[4];
        byte bysBirth[] = new byte[16];
        byte bysAddr[] = new byte[70];
        byte bysIdCode[] = new byte[36];
        byte bysIssue[] = new byte[30];
        byte bysBeginDate[] = new byte[16];
        byte bysEndDate[] = new byte[16];

        String name = null;
        String sex = null;
        String nationStr = null;
        String birth = null;
        String addr = null;
        String idCode = null;
        String issue = null;
        String beginDate = null;
        String endDate = null;

        int iTextSize = 0;
        int iPhotoSize = 0;

        int iFingerSize = 0;

        iTextSize = data[10] << 8 + data[11];
        iPhotoSize = data[12] << 8 + data[13];
        iFingerSize = data[14] << 8 + data[15];

        ///////////////////////////////////////////////////
        //截取数据

        iOffset = 14;

        //截取姓名
        j = 0;
        for (i = iOffset; i < (iOffset + 30); i++) {
            bysName[j] = data[i];
            j++;
        }
        name = new String(bysName, "UTF-16LE");
        name = name.replace(" ", "");
        iOffset += 30;

        //截取性别
        j = 0;
        for (i = iOffset; i < (iOffset + 2); i++) {
            bysSexCode[j] = data[i];
            j++;
        }
        String strSexCode = new String(bysSexCode, "UTF-16LE");
        if (strSexCode.equals("1")) sex = "男";
        else if (strSexCode.equals("2")) sex = "女";
        else if (strSexCode.equals("0")) sex = "未知";
        else if (strSexCode.equals("9")) sex = "未说明";
        iOffset += 2;

        //截取民族
        j = 0;
        for (i = iOffset; i < (iOffset + 4); i++) {
            bysNationCode[j] = data[i];
            j++;
        }
        String strNationCode = null;
        strNationCode = new String(bysNationCode, "UTF-16LE");
        nationStr = nation[Integer.valueOf(strNationCode) - 1];
        iOffset += 4;

        //截取生日
        j = 0;
        for (i = iOffset; i < (iOffset + 16); i++) {
            bysBirth[j] = data[i];
            j++;
        }
        birth = new String(bysBirth, "UTF-16LE");
        iOffset += 16;

        //截取地址
        j = 0;
        for (i = iOffset; i < (iOffset + 70); i++) {
            bysAddr[j] = data[i];
            j++;
        }
        addr = new String(bysAddr, "UTF-16LE");
        iOffset += 70;

        //截取身份证号
        j = 0;
        for (i = iOffset; i < (iOffset + 36); i++) {
            bysIdCode[j] = data[i];
            j++;
        }
        idCode = new String(bysIdCode, "UTF-16LE");
        iOffset += 36;

        //截取签发机关
        j = 0;
        for (i = iOffset; i < (iOffset + 30); i++) {
            bysIssue[j] = data[i];
            j++;
        }
        issue = new String(bysIssue, "UTF-16LE");
        iOffset += 30;

        //截取有效期开始日期
        j = 0;
        for (i = iOffset; i < (iOffset + 16); i++) {
            bysBeginDate[j] = data[i];
            j++;
        }
        beginDate = new String(bysBeginDate, "UTF-16LE");
        iOffset += 16;

        //截取有效期结束日期
        j = 0;
        for (i = iOffset; i < (iOffset + 16); i++) {
            bysEndDate[j] = data[i];
            j++;
        }

        if (bysEndDate[0] >= '0' && bysEndDate[0] <= '9') {
            endDate = new String(bysEndDate, "UTF-16LE");
        } else {
            endDate = new String(bysEndDate, "UTF-16LE");
        }
        iOffset += 16;

        idCardInfo.setName(name);
        idCardInfo.setSex(sex);
        idCardInfo.setAddress(addr);
        idCardInfo.setIdNum(idCode);
        idCardInfo.setBirth(birth);
        idCardInfo.setSignOffice(issue);
        idCardInfo.setUsefulStartDate(beginDate);
        idCardInfo.setUsefulEndDate(endDate);
        return idCardInfo;
    }

    private void iCCardRead() {

        // 打开服务
        int openResult = rfidInterface.open(0);
        if (0 != openResult) {
            readICCardFail("IC卡服务打开失败！");
            return;
        }

        // 非接卡模块上电
        int powerOn = rfidInterface.powerOn();
        if (0 != powerOn) {
            readICCardFail("非接卡模块上电失败！");
            rfidInterface.close();
            return;
        }

        // 非接卡寻卡
        int searchOn = rfidInterface.searchCardWithoutPowerOn(10);
        if (0 != searchOn) {
            readICCardFail("非接卡模块寻卡失败！");
            rfidInterface.powerOff();
            rfidInterface.close();
            return;
        }

        // 非接卡上电，并返回 ATR 信息
        RFIDcard readResult = rfidInterface.cardPowerOn(TIMEOUT);
        if (readResult == null || 0 != readResult.getErrCode() || !readResult.isFlag()) {
            readICCardFail("非接卡上电失败！");
            rfidInterface.powerOff();
            rfidInterface.close();
            return;
        }

        // 设置蜂鸣器
        systemInterface.beep(3, 0, TIMEOUT);

        readATR = HexDump.toHexString(readResult.getATR());
        if (readATR.length() >= 12){
            readATR = readATR.substring(4, readATR.length()).substring(0, 8);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Map<String, String> map = new HashMap<>();
                map.put("iccard", readATR);
                Object data = JSONObject.toJSON(map);
                mLoadingView.setICCardData(data);
            }
        });
        rfidInterface.cardPowerOff(TIMEOUT);
        rfidInterface.powerOff();
        rfidInterface.close();
    }

    // 读取身份证错误提示
    private void readIDCardFail(String text) {
        systemInterface.beep(3, 0, TIMEOUT);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                systemInterface.beep(3, 0, TIMEOUT);
            }
        }, 150);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    // 读取IC卡错误提示
    private void readICCardFail(String text) {
        systemInterface.beep(3, 0, TIMEOUT);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                systemInterface.beep(3, 0, TIMEOUT);
            }
        }, 150);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

}
