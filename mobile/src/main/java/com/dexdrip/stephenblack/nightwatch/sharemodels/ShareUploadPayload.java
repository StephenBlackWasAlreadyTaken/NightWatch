package com.dexdrip.stephenblack.nightwatch.sharemodels;

import com.dexdrip.stephenblack.nightwatch.sharemodels.ShareUploadableBg;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stephenblack on 3/19/15.
 */
public class ShareUploadPayload {
    @Expose
    public String SN;

    @Expose
    public Egv[] Egvs;

    @Expose
    public long TA = -5;

    public ShareUploadPayload(String sn, ShareUploadableBg bg) {
        this.SN = sn;
        List<Egv> egvList = new ArrayList<Egv>();
        egvList.add(new Egv(bg));
        this.Egvs = egvList.toArray(new Egv[egvList.size()]);
    }
}
