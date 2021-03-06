package com.nhb.dtu.protocol;

import java.util.Date;
import java.util.List;

import com.nhb.dtu.base.AbstractDevice;
import com.nhb.dtu.entity.ReceiptCollector;
import com.nhb.dtu.entity.ReceiptDevice;
import com.nhb.dtu.entity.ReceiptMeter;
import com.nhb.dtu.entity.SwitchStatus;
import com.nhb.dtu.enums.SwitchStatusEnum;
import com.nhb.dtu.service.data.SwitchStatusService;
import com.nhb.dtu.service.submeter.ReceiptDeviceService;
import com.nhb.utils.nhb_utils.context.SpringContextHolder;

public class AC_JLMK_ON extends AbstractDevice {

	public AC_JLMK_ON(ReceiptCollector receiptCollector, ReceiptMeter receiptMeter, ReceiptDevice receiptDevice) {
		this.receiptCollector = receiptCollector;
		this.receiptMeter = receiptMeter;
		this.receiptDevice = receiptDevice;
		buildWritingFrames();
	}

	// private boolean result;

	private String status;

	@Override
	public void buildWritingFrames() {

		int[] data = new int[22];
		// 起始位置SOI 1位
		data[0] = 0x7E;
		// 通讯版本号 2位
		data[1] = 0x31;
		data[2] = 0x30;
		// 表地址 2位
		String meterNo = Integer.toHexString(Integer.parseInt(receiptMeter.getMeterNo())).toUpperCase();
		if (meterNo.length() < 2) {
			meterNo = "0" + meterNo;
		}
		char[] meterChar = meterNo.toCharArray();
		data[3] = (byte) meterChar[0] & 0xFF;
		data[4] = (byte) meterChar[1] & 0xFF;
		// cid1
		data[5] = 0x32;
		data[6] = 0x43;
		// cid2
		data[7] = 0x39;
		data[8] = 0x34;
		// length 校验
		Integer lenId = Integer.parseInt("04", 16);
		int[] lchlSum = Verify.lchlSum(lenId);
		data[9] = lchlSum[0] & 0xFF;
		data[10] = lchlSum[1] & 0xFF;
		data[11] = lchlSum[2] & 0xFF;
		data[12] = lchlSum[3] & 0xFF;
		// data info
		data[13] = 0x44;
		data[14] = 0x31;
		data[15] = 0x30;
		data[16] = 0x31;

		// CHKSUM 校验和码
		int[] chkSum = Verify.chkSum(data[1] + data[2] + data[3] + data[4] + data[5] + data[6] + data[7] + data[8]
				+ data[9] + data[10] + data[11] + data[12] + data[13] + data[14] + data[15] + data[16]);
		data[17] = chkSum[0];
		data[18] = chkSum[1];
		data[19] = chkSum[2];
		data[20] = chkSum[3];

		// 结束码 1位
		data[21] = 0x0D;

		byte[] frame = new byte[data.length];
		for (int i = 0; i < data.length; i++) {
			frame[i] = (byte) data[i];
		}
		writingFrames.add(frame);

	}

	@Override
	public boolean analyzeFrame(byte[] frame) {

		int[] data = new int[frame.length];
		for (int i = 0; i < data.length; i++) {
			data[i] = frame[i] & 0xFF;
		}
		// TODO length、lchsum检验
		// 解析 RTN 为 00 ，返回数据成功
		String rtn = (char) data[7] + "" + (char) data[8];
		if (!rtn.equals("00")) {
			return false;
		}
		status = SwitchStatusEnum.ON.getKey();
		// result = true;
		return true;
	}

	@Override
	public void handleResult() {
		List<ReceiptDevice> devices = getReceiptDeviceService().findByMeterId(receiptDevice.getMeterId());
		for (ReceiptDevice device : devices) {
			SwitchStatus switchStatus = getSwitchStatusService().findById(device.getId());
			Date nowTime = new Date();
			if (null == switchStatus) {
				switchStatus = new SwitchStatus();
				switchStatus.setDeviceId(receiptDevice.getId());
			}
			switchStatus.setReadTime(nowTime);
			switchStatus.setStatus(status);
			getSwitchStatusService().save(switchStatus);
		}
	}

	private ReceiptDeviceService getReceiptDeviceService() {
		return SpringContextHolder.getBean("receiptDeviceService");
	}

	private SwitchStatusService getSwitchStatusService() {
		return SpringContextHolder.getBean("switchStatusService");
	}

}
