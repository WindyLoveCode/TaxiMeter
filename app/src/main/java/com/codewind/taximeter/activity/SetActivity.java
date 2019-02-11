package com.codewind.taximeter.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.codewind.taximeter.R;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
/**
 * 设置
 * */
@EActivity(R.layout.activity_set)
public class SetActivity extends BaseActivity {
    @ViewById(R.id.layout_set_back)
    LinearLayout layout_back;
    @ViewById(R.id.layout_set_sfbz)
    RelativeLayout layout_sfbz;
    @ViewById(R.id.layout_set_yxgj)
    RelativeLayout layout_yxgj;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    /**返回*/
    @Click(R.id.layout_set_back)
    void click_back(){
        finish();
    }
    /**收费标准*/
    @Click(R.id.layout_set_sfbz)
    void click_sfbz(){
        Intent intent = new Intent(SetActivity.this,SFBZActivity_.class);
        startActivity(intent);
    }
    /**运行轨迹*/
    @Click(R.id.layout_set_yxgj)
    void click_yxgj(){

    }
}
