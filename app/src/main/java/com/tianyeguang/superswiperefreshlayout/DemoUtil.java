package com.tianyeguang.superswiperefreshlayout;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

/**
 * Created by wangyeming on 2016/8/14.
 */
public class DemoUtil {

    public static int dp2px(Context context, int dip) {
        Resources resources = context.getResources();
        int px = Math.round(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, resources.getDisplayMetrics()));
        return px;
    }

}
