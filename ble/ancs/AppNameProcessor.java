package com.mstarc.commonbase.communication.bluetooth.ble.ancs.dataprocess;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/**
 * general ANCS Data Handler
 */
public class AppNameProcessor {
    private static final String TAG_LOG = "LeService";

    private String ds_app_id;
    private String ds_app_name;
    private ByteArrayOutputStream processing_attribute_value;

    private int remain_packet_size;
    private int remain_next_packet_byte;

    private int packageLength;

    private enum ds_notification_processing_status {
        app_id, app_name, finish
    }

    private ds_notification_processing_status processing_status;

    public AppNameProcessor() {
        processing_attribute_value = new ByteArrayOutputStream();
        init();
    }

    public void init() {
        processing_status = processing_status.app_id;
        remain_packet_size = 0;
        remain_next_packet_byte = 0;
        ds_app_id = null;
        ds_app_name = null;
        processing_attribute_value.reset();
    }

    public void processing(byte[] get_data) {
        remain_packet_size = get_data.length;

        int index_att_id;
        while (remain_packet_size > 0) {
            if (processing_status.equals(processing_status.app_id)) {
                //parse first packet include type, uid, app id length/value att1_id, length

                index_att_id = 1 + getPackageLength();

                //get att0's length
                int att_length = get_attribute_length(get_data, index_att_id);
                if (att_length > 30 || att_length < 0) {
                    processing_status = processing_status.finish;
                    processing_attribute_value.reset();
                    return;
                }
                Log.d(TAG_LOG, "Appname_att_length: " + att_length);

                int current_packet_att_length = get_data.length - (index_att_id + 3);
                Log.d(TAG_LOG, "Appname_current_packet_att_length: " + current_packet_att_length);

                if (current_packet_att_length < att_length) {
                    // fragmentations is occure
                    processing_attribute_value.write(get_data, index_att_id + 3, current_packet_att_length);
                    remain_next_packet_byte = att_length - current_packet_att_length;
                    remain_packet_size = 0;
                    processing_status = processing_status.app_name;
                    Log.d(TAG_LOG, "Appname_remain:: " + remain_next_packet_byte);
                } else {
                    //just finish reading app id
                    Log.d(TAG_LOG, att_length + " : " + current_packet_att_length);
                    processing_attribute_value.write(get_data, index_att_id + 3, att_length);
                    remain_packet_size = current_packet_att_length - att_length;
                    remain_next_packet_byte = 0;
                    processing_status = processing_status.app_name;
                    Log.d(TAG_LOG, "Appname_finish reading. remain:: " + remain_packet_size);

                    update_processing_status();
                }
                Log.d(TAG_LOG, "Appname_remain packet size: " + remain_packet_size);
                Log.d(TAG_LOG, "Appname_remain next packet size: " + remain_next_packet_byte);
            } else {
                //parse 1st packet remain or 2nd~packet( includefragment data)
                Log.d(TAG_LOG, "Appname_: remain_next_packet size: " + remain_next_packet_byte);

                //2nd~ packet.
                if (remain_next_packet_byte > 0) {
                    Log.d(TAG_LOG, "Appname_read fragment data" + remain_next_packet_byte);
                    Log.d(TAG_LOG, "Appname_remain_packet_size" + remain_packet_size);
                    //read fragment data, continue from pre packet
                    if (remain_next_packet_byte > remain_packet_size) {
                        //continue fragment
                        processing_attribute_value.write(get_data, 0, remain_packet_size);
                        remain_next_packet_byte -= remain_packet_size;
                        remain_packet_size = 0;
                        Log.d(TAG_LOG, "Appname_ fragment is continue. remain:: " + remain_next_packet_byte);
                    } else {
                        //just finish fragment
                        Log.d(TAG_LOG, "Appname_  remain next packet seizet:: " + remain_next_packet_byte);
                        processing_attribute_value.write(get_data, 0, remain_next_packet_byte);
                        remain_packet_size -= remain_next_packet_byte;
                        remain_next_packet_byte = 0;
                        Log.d(TAG_LOG, "Appname_  att value is just finish reading. remai_current_packet:: " +
                                remain_packet_size);

                        update_processing_status();
                    }
                } else {
                    Log.d(TAG_LOG, "Appname_ remain packet size: " + remain_packet_size);
                    Log.d(TAG_LOG, "Appname_ remain next packet size: " + remain_next_packet_byte);
                    //continue rading current packet data, or just finish reading data in pre packet
                    //continue reading current packet data
                    if (remain_packet_size > 0) {
                        //get next att's length
                        index_att_id = get_data.length - remain_packet_size;

                        //get next attr's length
                        int att_length = get_attribute_length(get_data, index_att_id);
                        Log.d(TAG_LOG, "Appname_ next att length: " + att_length);
                        if (att_length < 0) {
                            processing_status = processing_status.finish;
                            processing_attribute_value.reset();
                            return;
                        }
                        int current_packet_att_length = get_data.length - (index_att_id + 3);

                        //check fragment
                        if (current_packet_att_length < att_length) {
                            // fragmentation is occured
                            processing_attribute_value.write(get_data, index_att_id + 3,
                                    current_packet_att_length);
                            remain_next_packet_byte = att_length - current_packet_att_length;
                            remain_packet_size = 0;
                            Log.d(TAG_LOG, "Appname_ att value is fragment. remain:: " + remain_next_packet_byte);
                        } else {
                            // no fragmentation
                            Log.d(TAG_LOG, att_length + " : " + current_packet_att_length);
                            processing_attribute_value.write(get_data, index_att_id + 3, att_length);
                            remain_packet_size = current_packet_att_length - att_length;
                            remain_next_packet_byte = 0;
                            Log.d(TAG_LOG, "Appname_ app id value is just finish reading. remain:: " +
                                    remain_packet_size);

                            update_processing_status();
                        }
                    } else {
                        Log.d(TAG_LOG, "Appname_ ");
                    }
                }
            }
        }
    }


    public String get_ds_app_id() {
        return ds_app_id;
    }

    public String get_ds_app_name() {
        return ds_app_name;
    }


    public int getPackageLength() {
        return packageLength;
    }

    public void setPackageLength(int packageLength) {
        this.packageLength = packageLength;
    }

    public Boolean is_finish_processing() {
        return processing_status.equals(processing_status.finish);
    }


    private int get_attribute_length(byte[] _get_data, int _index_att) {
        //get att0's length
        byte[] byte_ds_length = {_get_data[_index_att + 2], _get_data[_index_att + 1]};
        BigInteger ds_length_big = new BigInteger(byte_ds_length);
        return ds_length_big.intValue();
    }

    private void update_processing_status() {
        switch (processing_status) {
            case app_id:
                Log.d(TAG_LOG, "Appname_ finish app ds_app_name reading.");
                processing_status = processing_status.app_name;
                try {
                    ds_app_id = new String(processing_attribute_value.toByteArray(), "UTF-8");
                    processing_attribute_value.reset();
                    Log.d(TAG_LOG, "Appname_ ds_app_id : " + ds_app_id);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case app_name:
                Log.d(TAG_LOG, "Appname_ finish title  reading.");
                processing_status = processing_status.finish;
                try {
                    ds_app_name = new String(processing_attribute_value.toByteArray(), "UTF-8");
                    processing_attribute_value.reset();
                    Log.d(TAG_LOG, "Appname_ appname : " + ds_app_name);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;

            default:
                Log.d(TAG_LOG, "Appname_.");
                break;
        }
    }
}
