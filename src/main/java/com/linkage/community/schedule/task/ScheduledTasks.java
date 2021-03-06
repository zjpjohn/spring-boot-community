package com.linkage.community.schedule.task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.linkage.community.schedule.dbutils.CommonDBTemplate;
import com.linkage.community.schedule.mail.MailSenderFactory;
import com.linkage.community.schedule.mail.MailSenderType;
import com.linkage.community.schedule.mail.SimpleMailSender;
import com.linkage.community.schedule.push.xinge.XingeAppPushUtil;
import com.linkage.community.schedule.utils.CommonHttpUtil;
import com.linkage.community.schedule.utils.Conver;
import com.linkage.community.schedule.utils.FileUtils;
import com.linkage.community.schedule.utils.WeimiSms;
import com.tencent.xinge.ClickAction;
import com.tencent.xinge.Message;
import com.tencent.xinge.MessageIOS;

/**
 * 社区APP后台定时任务
 * 
 * @author wangjie
 *
 */
@Configuration
@ImportResource("classpath:spring-beans.xml")
@EnableScheduling
public class ScheduledTasks {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	// private final Log log = LogFactory.getLog(getClass());//common-log
	// private final Logger log = Logger.getLogger(getClass());//log4j
	private final Logger log = LoggerFactory.getLogger(getClass());// slf4j
																	// --logback实现
	 /**
     * 商家信息Excel文件存放的文件路径
     */
    public static final String FILEPATH = "/tomcat/webapps/community/productImg";
    
    public static final String QINIU_IMG_HOST = "http://zlzwimg.qiniudn.com";
    
	//服务http地址
	private static String BASE_URI = "http://yunpian.com";
	//服务版本号
	public static String VERSION = "v1";
	//apikey
	public static final String apiKey = "88e037763b05c3f8839ccd3757e95060";
    //通用发短信接口
	public static String URI_SEND_SMS = BASE_URI + "/" + VERSION + "/sms/send.json";

	@Autowired
	private CommonDBTemplate commonDBTemplate;
	
	//@Autowired
	//private UserService userService;

	 @Autowired
	 private MemcachedClient memcachedClient;

	//@Scheduled(fixedRate=5000)
	public void reportCurrentTime() throws Exception {
		// Connection conn = DataSourceUtils.doGetConnection(dataSource);
		// System.out.println(conn);

		// DataSourceUtils.releaseConnection(conn, dataSource);
		//log.info("hi,this is logback");
		System.out.println("The time is now " + dateFormat.format(new Date()));
		//userService.findAll();
		
		/*
		 * List<User> list = userService.findAll(); for (User user : list) {
		 * System.out.println(user); }
		 */
		List<Map<String, Object>> list = this.commonDBTemplate
				.queryMapList("select * from user");
		for (Map<String, Object> map : list) {
			System.out.println(map);
		}
		this.commonDBTemplate.closeConnection();

	}

	// 每一分钟执行一次发送邮件任务
	@Scheduled(fixedDelay = 60000)
	public void sendMessage() {
		long begintime = System.currentTimeMillis();
		String sql = "select phone_num,verification_code,error_msg from verification_code where"
				+ " generate_time< timestampadd(minute, -3, current_timestamp) "
				+ " and generate_time> timestampadd(minute, -4, current_timestamp) and (state is null or state!='SUCCESS') "
				+ " and date(generate_time)=date(sysdate()) order by generate_time desc";
		HashMap[] list = this.commonDBTemplate.queryMapList(sql).toArray(
				new HashMap[0]);
		List<String> recipients = new ArrayList<String>();
		recipients.add("785592116@qq.com");
		recipients.add("xueqt@asiainfo.com");
		try {
			if (list != null && list.length > 0) {
				sendClaimMail(list, recipients);
			} else {

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.commonDBTemplate.closeConnection();
		long endtime = System.currentTimeMillis();
		log.info("定时发送短信超时提醒邮件成功,用时"+(endtime-begintime)+"毫秒！");
	}
	
	
	//@Scheduled(fixedRate=10000)
	public void pushPersonMessage() throws Exception
	{
		sendPersonMessage("153","宅里宅外秒杀活动提醒","栖庭专场-开年抢个好兆头，价值76元的吉丽嘉多双人下午茶限量0元秒杀，活动详情请您查看首页通告。");
		sendPersonMessage("51","宅里宅外秒杀活动提醒","栖庭专场-开年抢个好兆头，价值76元的吉丽嘉多双人下午茶限量0元秒杀，活动详情请您查看首页通告。");
	}
	
	//@Scheduled(cron="0 47 17 23 12 ?")
	public void search()
	{
		String value1 = memcachedClient.get("seckill_15_begin").toString();
		String value2 = memcachedClient.get("seckill_15_end").toString();
		String value3 = memcachedClient.get("seckill_15_count").toString();
		System.out.println("seckill_15_begin=" + value1);
		System.out.println("seckill_15_end=" + value2);
		System.out.println("seckill_15_count=" + value3);
	}
	
	/*@Scheduled(cron="0 0 17 30 12 ?")
	public void pushMessage()
	{
		sendSysMessage("宅里宅外秒杀活动提醒","栖庭专场-开年抢个好兆头，价值76元的吉丽嘉多双人下午茶限量0元秒杀，活动详情请您查看首页通告。",dateFormat.format(new Date()));
	}
	
	@Scheduled(cron="0 55 9 31 12 ?")
	public void pushMessage2()
	{
		sendSysMessage("宅里宅外秒杀活动提醒","栖庭用户请注意哦，马上开抢！您与双人下午茶套餐的距离只是手指与屏幕的距离。请您点击首页秒杀专区。",dateFormat.format(new Date()));
	}*/
	
	
	/*@Scheduled(cron="0 18 20 23 12 ?")
	public void pushMessage()
	{
		sendSysMessage("宅里宅外秒杀活动提醒","宅里宅外栖庭专场—圣诞豪礼0元秒杀，458元写真、儿童理发、美容项目整点开抢。",dateFormat.format(new Date()));
	}
	
	@Scheduled(cron="0 10 10 24 12 ?")
	public void pushMessage2()
	{
		sendSysMessage("宅里宅外秒杀活动提醒","栖庭用户注意啦！给小孩留下这个冬天最美的自己，专业儿童写真套系11点0元开抢！详细活动请于首页查看。",dateFormat.format(new Date()));
	}
	
	@Scheduled(cron="0 55 19 24 12 ?")
	public void pushMessage3()
	{
		sendSysMessage("宅里宅外秒杀活动提醒","栖庭用户注意啦！平安夜给孩子再填一份礼物吧，价值458元专业写真20点0元开抢。详细活动请于首页查看。20点开抢！",dateFormat.format(new Date()));
	}
	
	@Scheduled(cron="0 55 15 25 12 ?")
	public void pushMessage4()
	{
		sendSysMessage("宅里宅外秒杀活动提醒","栖庭用户注意啦！16点准点开抢，带着小孩免费去理发！详细活动请于首页查看。",dateFormat.format(new Date()));
	}
	
	@Scheduled(cron="0 55 19 25 12 ?")
	public void pushMessage5()
	{
		sendSysMessage("宅里宅外秒杀活动提醒","栖庭用户注意啦！圣诞快结束了，对老婆、对妈妈好一点，20点美容礼包抢了送给她！详细活动请于首页查看。",dateFormat.format(new Date()));
	}*/
	
	//@Scheduled(fixedDelay=10000)
	@Scheduled(cron="0 30 14 * * ?")
	public void sendAdminSms()
	{
		String sql = "select count(1) FROM orders_management o left join delivery_config d on d.delivery_type_id=o.delivery_type_id where o.order_state=1 and d.is_delivery=1";
		int count = this.commonDBTemplate.queryCount(sql);
		String str = "尊敬的管理员您好，系统有"+count+"笔订单已付款未发货，请及时发货。(此验证码有效期为5分钟)，如非本人操作，请忽略本短信。【宅里宅外】";
		String str2 = "尊敬的管理员您好，系统有"+count+"笔订单已付款未发货，请及时发货。";
		String smstype =  Conver.convertNull(memcachedClient.get("smstype2"));
		if(count > 0)
		{
			if(smstype.equals("1")){
	        	smsService(Long.parseLong("18951765458"), str);
	        }else{
	        	smsWeimi(Long.parseLong("18951765458"), str2);
	        }
		}
		this.commonDBTemplate.closeConnection();
	}
	
	 public boolean smsService(Long phoneNum, String content)
	    {
		 long begintime = System.currentTimeMillis();
	        boolean result = false;
	        HttpClient client = new HttpClient();
	        NameValuePair[] nameValuePairs = new NameValuePair[3];
	        nameValuePairs[0] = new NameValuePair("apikey", apiKey);
	        nameValuePairs[1] = new NameValuePair("text", content);
	        nameValuePairs[2] = new NameValuePair("mobile", String.valueOf(phoneNum));
	        PostMethod method = new PostMethod(URI_SEND_SMS);
	        method.setRequestBody(nameValuePairs);
	        HttpMethodParams param = method.getParams();
	        param.setContentCharset("UTF-8");
	        try {
	            client.executeMethod(method);
	            JSONObject json=JSONObject.parseObject(method.getResponseBodyAsString());
	            if(0==json.getIntValue("code"))
	            {
	            	result = true;
	            	log.info("通过云片发送短信成功:"+json.toJSONString());
	            }
	            else 
	            {
	                result = false;
	                log.info("通过云片发送短信失败:"+json.toJSONString());
	            }
	        } catch (HttpException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        long endtime = System.currentTimeMillis();
			log.info("通过云片发送短信,用时"+(endtime-begintime)+"毫秒！");
	        return result;
	    }
	    
	    public boolean smsWeimi(Long phoneNum, String content) {
	    	long begintime = System.currentTimeMillis();
	    	boolean result = false;
	    	String[][] data = null;
	        ArrayList<String[]> para = new ArrayList<String[]>();
	        para.add(new String[] { "cid", "8FVqSlzvKXFF" });
	        para.add(new String[] { "mob", phoneNum.toString() });
	        para.add(new String[] { "uid", "8xHNZ03Rd447" });
	        para.add(new String[] { "pas", "cmnk9z2v" });
	        para.add(new String[] { "p1", content });
	        para.add(new String[] { "type", "json" });
	        data = new String[para.size()][];
	        para.toArray(data);
	        JSONObject json=JSONObject.parseObject(WeimiSms.postHTML("http://api.weimi.cc/2/sms/send.html",data, 3, "UTF-8"));
			int code = json.getIntValue("code");
			if(0 == code){
				log.info("通过微米发送短信成功:"+json.toJSONString());
	            result = true;
			}else{
				log.info("通过微米发送短信失败:"+json.toJSONString());
				result = false;
			}
			long endtime = System.currentTimeMillis();
			log.info("通过微米发送短信,用时"+(endtime-begintime)+"毫秒！");
	        return result;
		}
	    
	    
	public void sendPersonMessage(String user_id,String title,String content) throws Exception
	{
		long begintime = System.currentTimeMillis();
		
		String dataStr = dateFormat.format(new Date());
		//插入推送的系统消息
		StringBuffer sb = new StringBuffer();
		sb.append("insert into push_message(push_title,push_content,push_type,message_type,push_source,push_target,push_url,push_state,push_time) ")
		.append("select '"+title+"' as push_title,'"+content+"' as push_content,")
		.append("3 as push_type,1 as message_type,'-1' as push_source,user_id as user_id,null as push_url,'0' as push_state,'"+dataStr+"' as push_time ")
		.append("from register_user ")
		.append("where user_id="+user_id);
		this.commonDBTemplate.saveOrUpdateOrDelete(sb.toString());
		//推送消息
		//封装custom
    	Map<String, Object> custom = new HashMap<String, Object>();
		custom.put("type", "1");
		String sql = "select t1.user_state,t1.phone_number,t2.phone_platform from register_user t1,user_info t2 where  t1.user_id=t2.user_id and t1.user_id=?";
		Map<String, Object> map = this.commonDBTemplate.queryOneMap(sql, new Object[]{Integer.parseInt(user_id)});
		String phone_number = Conver.convertNull(map.get("phone_number"));
		String phone_platform = Conver.convertNull(map.get("phone_platform"));
		String user_state = Conver.convertNull(map.get("user_state"));
		org.json.JSONObject jsonObject = null;
		String push_state = "";
		if(user_state.equals("1")){
		    //IOS推送
    		if(phone_platform.equals("1"))
    		{
    			if(content.getBytes("UTF-8").length>140){
    				content = substring(content,140,"...");
    			}
    			//content = title +":"+ content;
    			MessageIOS message = XingeAppPushUtil.constructMessageIOS(title,content, custom);
    			//单个发送
    			jsonObject = XingeAppPushUtil.pushSingleAccountIOS(message, phone_number);
    		}
    		else //ANDROID推送
    		{
    			Message  message = XingeAppPushUtil.constructMessageAndroid(Message.TYPE_NOTIFICATION, 1, ClickAction.TYPE_INTENT, title, content,"intent:#Intent;component=com.linkage.community/.ucenter.NotifyActivity;S.type=1;end", custom);
    			jsonObject = XingeAppPushUtil.pushSingleAccountAndriod(message, phone_number);
    		}
    		push_state = jsonObject.get("ret_code").toString();
    		log.info("消息推送状态："+push_state);
		}
		this.commonDBTemplate.closeConnection();
		long endtime = System.currentTimeMillis();
		log.info("消息发送给用户标识为:"+user_id+"的用户成功,用时"+(endtime-begintime)+"毫秒！");
	}
	
	public void sendSysMessage(String title,String content,String dataStr)
	{
		long begintime = System.currentTimeMillis();
		//插入推送的系统消息
		StringBuffer sb = new StringBuffer();
		sb.append("insert into push_message(push_title,push_content,push_type,message_type,push_source,push_target,push_url,push_state,push_time) ")
		.append("select '"+title+"' as push_title,'"+content+"' as push_content,")
		.append("3 as push_type,1 as message_type,'-1' as push_source,user_id as user_id,null as push_url,'0' as push_state,'"+dataStr+"' as push_time ")
		.append("from register_user");
		this.commonDBTemplate.saveOrUpdateOrDelete(sb.toString());
		//推送消息
		Map<String, Object> custom = new HashMap<String, Object>();
		custom.put("type", "1");
		Message andriodMessage = XingeAppPushUtil.constructMessageAndroid(Message.TYPE_NOTIFICATION, 1, ClickAction.TYPE_INTENT, title,content ,"intent:#Intent;component=com.linkage.community/.ucenter.NotifyActivity;S.type=1;end", custom);
		MessageIOS iosMessage = XingeAppPushUtil.constructMessageIOS(title,content, custom);
		org.json.JSONObject jsonObject = XingeAppPushUtil.pushAllDeviceAndroid(andriodMessage);
		org.json.JSONObject jsonObject1 = XingeAppPushUtil.pushAllDeviceIOS(iosMessage);
		try {
			log.info("安卓消息推送状态："+jsonObject.get("ret_code").toString());
			log.info("IOS消息推送状态："+jsonObject1.get("ret_code").toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.commonDBTemplate.closeConnection();
		long endtime = System.currentTimeMillis();
		log.info("消息群推成功,用时"+(endtime-begintime)+"毫秒！");
	}
	
	 //根据字节数截取字符串
    public static String substring(String str, int toCount,String more) throws UnsupportedEncodingException
	 {
		 int reInt = 0;
		 String reStr = "";
		 if (str == null)
		 return "";
		 char[] tempChar = str.toCharArray();
		 for (int kk = 0; (kk < tempChar.length && toCount > reInt); kk++) {
			 String s1 = str.valueOf(tempChar[kk]);
			 byte[] b = s1.getBytes("UTF-8");
			 reInt += b.length;
			 reStr += tempChar[kk];
		 }
		 if (toCount == reInt || (toCount == reInt - 1))
			 reStr += more;
		 return reStr;
	 }
	
	
	@Scheduled(cron="0 0 * * * ?")
	public void weatherTask()
	{
		long begintime = System.currentTimeMillis();
		ArrayList list = new ArrayList();
		List<Map<String, Object>> citys = commonDBTemplate.queryMapList("select t.req_code,t.city_name from  weather_config t");
		Map<String,Object> map = null;
		CloseableHttpClient httpClient = CommonHttpUtil.getHttpClientInstance();
		
		for(int i=0;i<citys.size();i++){
			map = citys.get(i);
			//天气
			String url1 = "http://www.weather.com.cn/data/cityinfo/"+map.get("req_code")+".html";
			
			HttpGet getMethod = CommonHttpUtil.getGetMethod(url1, new ArrayList<BasicHeader>());
			JSONObject result = CommonHttpUtil.callHttpClient2(httpClient, getMethod);
			//PM2.5
			String cityName=map.get("city_name").toString().replace("市","");
			String url2 = "http://www.pm25.in/api/querys/pm2_5.json?city="+cityName+"&token=PpJwsr28BMgPW2FFrdx3";
			System.out.println(cityName);
			HttpGet getMethod1 = CommonHttpUtil.getGetMethod(url2, new ArrayList<BasicHeader>());
			JSONArray pmresult = CommonHttpUtil.callHttpClient4(httpClient, getMethod1);
			if(pmresult!=null&&result!=null){
				JSONObject weatherinfo = JSONObject.parseObject(result.get("weatherinfo").toString());
				JSONObject pm = JSONObject.parseObject(pmresult.get(pmresult.size()-1).toString());
				String city_code=weatherinfo.get("cityid").toString();
				String weather=weatherinfo.get("weather").toString();
				String temp1=weatherinfo.get("temp1").toString();
				String temp2=weatherinfo.get("temp2").toString();
				String pm2_5=pm.getString("pm2_5").toString();
				String quality=pm.getString("quality").toString();
				Object[] param = new Object[]{weather,temp1,temp2,pm2_5,quality,city_code};
				list.add(param);
			}
		}
	  Object[][] params2 = new Object[list.size()][];
      for(int k=0;k<list.size();k++)
      	params2[k] = (Object[])list.get(k);
      this.commonDBTemplate.batchUpdate("update weather_config set weather=?,temp1=?,temp2=?,pm2_5=?,quality=? where req_code=?", params2);
      try {
		httpClient.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	this.commonDBTemplate.closeConnection();
	long endtime = System.currentTimeMillis();
	log.info("同步天气信息成功,用时"+(endtime-begintime)+"毫秒！");
		
	}
	
	//每天凌晨3点执行一次商家信息导入操作
	//@Scheduled(cron="0 0 3 * * ?")
	public void importSupplyerInfo()
	{
		long begintime = System.currentTimeMillis();
		 CloseableHttpClient httpClient = CommonHttpUtil.getHttpClientInstance();
	        HttpGet getMethod = null;
	        Document doc = null;
	        org.dom4j.Element totalEle = null;
	        org.dom4j.Element rootElement = null;
	        org.dom4j.Element elements = null;
	        String url2 = "";
	        String keyword2 = "";
	        String area2= null;
	        
	        String cityName = "";
	        
	        String sql = "insert into supplyer_config(area_code,supplyer_name,supplyer_address,type_id,supplyer_phone,supplyer_bushour,supplyer_pic,supplyer_lng,supplyer_lat) values (?,?,?,?,?,?,?,?,?)";
	        ArrayList paramsList = new ArrayList();
	        //1、遍历文件存放目录，没有文件则直接返回
	        File fileDir = new File(FILEPATH);
	        String[] fileList = fileDir.list();
	        
	        if(null == fileList || 0 == fileList.length)
	        {
	            return;
	        }
	        String[][] supplyerInfoArr = null;
	        Object[] params = null;
	        
	        //2、读取出文件列表，将Excel表格中数据读取到内存中
	        for(int i=0;i<fileList.length;i++)
	        {
	            if(-1 != fileList[i].indexOf(".xlsx"))
	            {
	                try
	                {
	                    supplyerInfoArr = getDataXlsx(fileList[i], 1);
	                }
	                catch (IOException e)
	                {
	                    log.error("读取Excel2007+文件:"+ fileList[i] +" 异常", e);
	                }
	            }
	            else if(-1 != fileList[i].indexOf(".xls"))
	            {
	                try
	                {
	                    supplyerInfoArr = getDataXls(fileList[i], 1);
	                }
	                catch (FileNotFoundException e)
	                {
	                    log.error("未找到文件:"+ fileList[i], e);
	                }
	                catch (IOException e)
	                {
	                    log.error("读取Excel2003文件:"+ fileList[i] +" 异常", e);
	                }
	            }
	            else
	            {
	                continue;
	            }
	            
	            for(int j=0;j<supplyerInfoArr.length;j++)
	            {
	                params = new Object[supplyerInfoArr[j].length+1];
	                
	                for(int m=0;m<supplyerInfoArr[j].length-1;m++)
	                {
	                    //商家图标拼接
	                    if(supplyerInfoArr[j].length-2 == m)
	                    {
	                        supplyerInfoArr[j][m] = QINIU_IMG_HOST + "/" + supplyerInfoArr[j][m] + ".jpg";
	                    }
	                    
	                    params[m]=supplyerInfoArr[j][m];
	                }
	                //字符串截取出该记录所在的城市名称
	                if(-1 == supplyerInfoArr[j][2].indexOf("省") || -1 == supplyerInfoArr[j][2].indexOf("市"))
	                {
	                    cityName = "南京";
	                }
	                else
	                {
	                    cityName = supplyerInfoArr[j][2].substring(supplyerInfoArr[j][2].indexOf("省")+1, supplyerInfoArr[j][2].indexOf("市"));
	                }
	                
	                try
	                {
	                    area2 = URLEncoder.encode(cityName,"UTF-8");
	                }
	                catch (UnsupportedEncodingException e1)
	                {
	                    log.error("", e1);
	                }
	                
	                //根据商家地址反地址解析出商家的经纬度
	                int totalNum = 0;
	                try
	                {
	                    keyword2 = URLEncoder.encode(supplyerInfoArr[j][2],"UTF-8");
	                }
	                catch (UnsupportedEncodingException e)
	                {
	                    log.error("", e);
	                }
	                url2 = CommonHttpUtil.BAIDU_LBS_URL.replace("QUERY", keyword2).replace("REGION", area2).replace("PAGESIZE", "1").replace("PAGENUM", "0");
	                getMethod = CommonHttpUtil.getGetMethod(url2, new ArrayList<BasicHeader>());
	                doc = CommonHttpUtil.callHttpClient(httpClient, getMethod);
	                if(null == doc)
	                {
	                    continue;
	                }
	                if(null == doc.selectSingleNode("/PlaceSearchResponse/total"))
	                {
	                    continue;
	                }
	                totalEle = (org.dom4j.Element)doc.selectSingleNode("/PlaceSearchResponse/total");
	                if(null == totalEle)
	                {
	                    continue;
	                }
	                totalNum = Integer.parseInt(Conver.convertNull(totalEle.getText()));
	                if(totalNum < 0)
	                {
	                    continue;
	                }
	                rootElement = doc.getRootElement();
	                elements = (org.dom4j.Element)rootElement.element("results").elements("result").get(0);
	                
	                params[params.length-2] = Conver.convertDouble(elements.element("location").elementText("lng"));
	                params[params.length-1] = Conver.convertDouble(elements.element("location").elementText("lat"));
	                paramsList.add(params);
	            }
	            
	            Object[][] params2 = new Object[paramsList.size()][];
	            for(int k=0;k<paramsList.size();k++)
	            	params2[k] = (Object[])paramsList.get(k);
	            this.commonDBTemplate.insertBatch(sql, params2);
	            
	            //if(SQLCommand.executeSQLs(sql, paramsList))
	           // {
	                //一个文件入表完成后，将参数ArrayList清空
	                paramsList.clear();
	                File toDelFile = new File(FILEPATH + File.separator + fileList[i]);
	                try
	                {
	                    FileUtils.copyFile(FILEPATH + File.separator + fileList[i], FILEPATH.replace("productImg", "upload") + File.separator + fileList[i]);
	                    toDelFile.delete();
	                }
	                catch (IOException e)
	                {
	                    log.error("", e);
	                }
	           // }
	        }
	        this.commonDBTemplate.closeConnection();
	        long endtime = System.currentTimeMillis();
	        log.info("执行导入商家信息excel成功,用时"+(endtime-begintime)+"毫秒。");
	}
	
	
	//每10分钟执行一次OCS同步
	@Scheduled(fixedDelay = 600000)
	public void synOCSMemory()
	{
		long begintime = System.currentTimeMillis();
		saveSupplyerByTypeNew();
		saveSupplyer();
		saveProducts();
		saveSupplyerByType();
		this.commonDBTemplate.closeConnection();
		long endtime = System.currentTimeMillis();
	    log.info("执行OCS同步刷新成功,用时"+(endtime-begintime)+"毫秒。");
	}
	
	@Scheduled(cron="0 55 * * * ?")
	public void synOCSSeckill()
	{
		long begintime = System.currentTimeMillis();
		try {
			log.info("加载秒杀信息...");
			if(memcachedClient == null) {
				System.out.println("memcachedClient is null...");
				return;
			}
			//将秒杀活动信息放入到ocs中
			String sql = "select seckill_id, unix_timestamp(seckill_begin) as seckill_begin, unix_timestamp(seckill_end) as seckill_end, seckill_count from seckill where unix_timestamp(seckill_end) >= unix_timestamp(sysdate())";
			Map[] seckills = this.commonDBTemplate.queryMapList(sql).toArray(new HashMap[0]);
			for(Map seckill : seckills) {
				String seckill_id = seckill.get("seckill_id").toString();
				String key_seckill_begin = "seckill_" + seckill_id + "_begin";
				String key_seckill_end = "seckill_" + seckill_id + "_end";
				String key_seckill_user = "seckill_" + seckill_id + "_user";
				String key_seckill_count = "seckill_" + seckill_id + "_count";
				//秒杀开始时间
				//System.out.println("put key_seckill_begin..." + seckill.get("seckill_begin").toString());
				memcachedClient.set(key_seckill_begin, 0, seckill.get("seckill_begin").toString());
				//秒杀结束时间
				//System.out.println("put key_seckill_end..." + seckill.get("seckill_end").toString());
				memcachedClient.set(key_seckill_end, 0, seckill.get("seckill_end").toString());
				//秒杀剩余数量
				//System.out.println("put key_seckill_user..." + seckill.get("seckill_remain").toString());
				int count = Conver.convertInt(seckill.get("seckill_count").toString());
		        this.memcachedClient.set(key_seckill_count, 0, Integer.valueOf(count));
				//已秒杀成功的用户
				ArrayList seckilled = new ArrayList(count);
				String sql2 = "select user_id from  orders_management where seckill_id=?";
				Object[] params2 = {seckill_id};
				Map[] seckill_users = this.commonDBTemplate.queryMapList(sql2,params2).toArray(new HashMap[0]);
				for(Map seckill_user : seckill_users ) {
					seckilled.add(seckill_user.get("user_id").toString());
				}
				memcachedClient.set(key_seckill_user, 0, seckilled);
			}
			
			log.info("成功加载秒杀信息....");
		}
		catch(Exception e) {
			System.out.println("memcached Exception...");
			e.printStackTrace();
		}
		this.commonDBTemplate.closeConnection();
		long endtime = System.currentTimeMillis();
	    log.info("执行秒杀信息加载成功,用时"+(endtime-begintime)+"毫秒。");
	}
	
	
	public void saveSupplyerByTypeNew(){
		String citycode = "320100";
		String sql = "select type_id,merge_ids from supplyer_type_config where state=1 order by order_id";
		HashMap[] list = this.commonDBTemplate.queryMapList(sql).toArray(new HashMap[0]);
		for(int i=0;i<list.length;i++)
		{
			String type = Conver.convertNull(list[i].get("type_id"));
			String type_ids = "";
			if(!"".equals(Conver.convertNull(list[i].get("merge_ids"))))
			{
				type_ids = Conver.convertNull(list[i].get("merge_ids"));
			}
			else
			{
				type_ids = type;
			}
			String querysql = "select distinct s.supplyer_id,s.supplyer_name,ifnull(s.is_attestation,0) is_attestation,ifnull(s.is_order,0) is_order,ifnull(s.has_ad_info,0) has_ad_info,ifnull(s.is_zhai_offer,0) is_zhai_offer,s.supplyer_phone,s.supplyer_appraise,s.appraise_cnt,s.tel_cnt,s.supplyer_lat,s.supplyer_lng,s.supplyer_pic,s.is_wholecity,s.is_top,ad.supplyer_ad_info,ad2.supplyer_ad_info zhai_offer_info " 
		            +"from supplyer_config s " 
		            +"left join supplyer_ads ad on s.supplyer_id=ad.supplyer_id and ad.ad_id=(select max(ad3.ad_id) from supplyer_ads ad3 where ad3.ad_type=1 and ad3.supplyer_id=ad.supplyer_id) "
		            +"left join supplyer_ads ad2 on s.supplyer_id=ad2.supplyer_id and ad2.ad_id=(select max(ad4.ad_id) from supplyer_ads ad4 where ad4.ad_type=2 and ad4.supplyer_id=ad2.supplyer_id) "
		            +"where s.type_id in ("+type_ids+") and concat(substring(s.area_code,1,4),'00')=? and s.supplyer_state=1";
			Object[] params = {citycode};
			HashMap[] supplyerList = this.commonDBTemplate.queryMapList(querysql, params).toArray(new HashMap[0]);
			memcachedClient.set(citycode+"supplyer"+type+"newtype",1000, supplyerList);
		}
		
	}
	
	public void saveSupplyerByType(){
		String citycode = "320100";
		String sql = "select type_id from supplyer_type_config";
		HashMap[] list = this.commonDBTemplate.queryMapList(sql).toArray(new HashMap[0]);
		for(int i=0;i<list.length;i++){
			String type = (String) list[i].get("type_id");
			String querysql = "select distinct s.supplyer_id,s.supplyer_name,ifnull(s.is_attestation,0) is_attestation,ifnull(s.is_order,0) is_order,ifnull(s.has_ad_info,0) has_ad_info,ifnull(s.is_zhai_offer,0) is_zhai_offer,s.supplyer_phone,s.supplyer_appraise,s.appraise_cnt,s.tel_cnt,s.supplyer_lat,s.supplyer_lng,s.supplyer_pic,s.is_wholecity,s.is_top,ad.supplyer_ad_info,ad2.supplyer_ad_info zhai_offer_info " 
		            +"from supplyer_config s " 
		            +"left join supplyer_ads ad on s.supplyer_id=ad.supplyer_id and ad.ad_id=(select max(ad3.ad_id) from supplyer_ads ad3 where ad3.ad_type=1 and ad3.supplyer_id=ad.supplyer_id) "
		            +"left join supplyer_ads ad2 on s.supplyer_id=ad2.supplyer_id and ad2.ad_id=(select max(ad4.ad_id) from supplyer_ads ad4 where ad4.ad_type=2 and ad4.supplyer_id=ad2.supplyer_id) "
		            +"where s.type_id=? and concat(substring(s.area_code,1,4),'00')=? and s.supplyer_state=1";
			Object[] params = {Conver.convertInt(type),citycode};
			HashMap[] supplyerList = this.commonDBTemplate.queryMapList(querysql, params).toArray(new HashMap[0]);
			memcachedClient.set(citycode+"supplyer"+type+"type",1000, supplyerList);
		}
		
	}
	
	public void saveSupplyer(){
		String citycode = "320100";
		String querySql = "select 1 pic_type,s.supplyer_id,s.supplyer_name,s.supplyer_lat,s.supplyer_lng,ifnull(s.supplyer_zhai_pic,s.supplyer_pic) supplyer_zhai_pic,s.scale,"
			+"ifnull(s.is_zhai_offer,0) is_zhai_offer,ifnull(s.is_fetch,0) is_fetch,ad.supplyer_ad_info zhai_offer_info,unix_timestamp(s.zhai_pic_time) update_date " 
			+"from supplyer_config s "
			+"left join supplyer_ads ad on s.supplyer_id=ad.supplyer_id and ad.ad_id=(select max(ad_id) from supplyer_ads where ad_type=2 and supplyer_id=ad.supplyer_id) "
			+"where concat(substring(s.area_code,1,4),'00')=? and s.supplyer_state=1 and s.is_zhai_offer=1 and ad.supplyer_ad_info is not null and (s.supplyer_pic is not null or s.supplyer_zhai_pic is not null) "
			+"order by s.zhai_pic_time desc";
		Object[] params = new Object[]{citycode};
		//获取商家信息
		HashMap[] supplyerList = this.commonDBTemplate.queryMapList(querySql, params).toArray(new HashMap[0]);
		memcachedClient.set(citycode+"supplyer",1000, supplyerList);
	}
	
	public void saveProducts(){
		String citycode = "320100";
		String  querySql = "select 2 pic_type,s.supplyer_id,s.supplyer_name,s.supplyer_lat,s.supplyer_lng,"
			 +"pro.product_id,pro.product_name,pro.product_pic,pro.scale,pro.product_price,ifnull(pro.is_sale, 0) is_sale,pro.privilege_price,pro.product_remark,unix_timestamp(pro.update_date) update_date "
			 +"from (select * from product_info where product_state= 1 and is_sale = 1 and product_remark is not null order by update_date desc) pro "
			 +"join supplyer_config s on s.supplyer_id=pro.supplyer_id and concat(substring(s.area_code,1,4),'00')=? and s.supplyer_state=1 "
			 +"group by pro.supplyer_id "
			 +"order by pro.update_date desc";
		Object[] params = new Object[]{citycode};
		//获取商家信息
		HashMap[] productList = this.commonDBTemplate.queryMapList(querySql, params).toArray(new HashMap[0]);
		memcachedClient.set(citycode+"product",1000, productList);
	}
	
	/**
     * 
     * 读取Excel的内容，第一维数组存储的是一行中格列的值，二维数组存储的是多少个行
     * @param file 读取数据的源Excel
     * @param ignoreRows 读取数据忽略的行数，比喻行头不需要读入 忽略的行数为1
     * @return 读出的Excel中数据的内容
     * @throws FileNotFoundException
     * @throws IOException
     */
    
    public String[][] getDataXls(String fileStr, int ignoreRows)
        
        throws FileNotFoundException, IOException
    {
        File file = new File(FILEPATH + File.separator + fileStr);
        List<String[]> result = new ArrayList<String[]>();
        int rowSize = 0;
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        
        // 打开HSSFWorkbook
        POIFSFileSystem fs = new POIFSFileSystem(in);
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        HSSFCell cell = null;
        
        for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); sheetIndex++)
        {
            HSSFSheet st = wb.getSheetAt(sheetIndex);
            
            // 第一行为标题，不取
            for (int rowIndex = ignoreRows; rowIndex <= st.getLastRowNum(); rowIndex++)
            {
                HSSFRow row = st.getRow(rowIndex);
                
                if (row == null)
                {
                    continue;
                }
                int tempRowSize = row.getLastCellNum() + 1;
                if (tempRowSize > rowSize)
                {
                    rowSize = tempRowSize;
                }
                String[] values = new String[rowSize];
                Arrays.fill(values, "");
                boolean hasValue = false;
                for (short columnIndex = 0; columnIndex <= row.getLastCellNum(); columnIndex++)
                {
                    String value = "";
                    cell = row.getCell(columnIndex);
                    if (cell != null)
                    {
                        // 注意：一定要设成这个，否则可能会出现乱码
                        
                        // cell.setEncoding(HSSFCell.ENCODING_UTF_16);
                        switch (cell.getCellType())
                        {
                            case HSSFCell.CELL_TYPE_STRING:
                                value = cell.getStringCellValue();
                                break;
                            case HSSFCell.CELL_TYPE_NUMERIC:
                                if (HSSFDateUtil.isCellDateFormatted(cell))
                                {
                                    Date date = cell.getDateCellValue();
                                    
                                    if (date != null)
                                    {
                                        value = new SimpleDateFormat("yyyy-MM-dd").format(date);
                                    }
                                    else
                                    {
                                        value = "";
                                    }
                                }
                                else
                                {
                                    value = new DecimalFormat("0").format(cell.getNumericCellValue());
                                }
                                break;
                            case HSSFCell.CELL_TYPE_FORMULA:
                                // 导入时如果为公式生成的数据则无值
                                if (!cell.getStringCellValue().equals(""))
                                {
                                    value = cell.getStringCellValue();
                                }
                                else
                                {
                                    value = cell.getNumericCellValue() + "";
                                }
                                break;
                            case HSSFCell.CELL_TYPE_BLANK:
                                break;
                            case HSSFCell.CELL_TYPE_ERROR:
                                value = "";
                                break;
                            case HSSFCell.CELL_TYPE_BOOLEAN:
                                value = (cell.getBooleanCellValue() == true ? "Y"
                                : "N");
                                break;
                            default:
                                value = "";
                        }
                    }
                    
                    /*if (columnIndex == 0 && value.trim().equals(""))
                    {
                        break;
                    }*/
                    values[columnIndex] = rightTrim(value);
                    hasValue = true;
                }
                if (hasValue)
                {
                    result.add(values);
                }
            }
        }
        
        in.close();
        String[][] returnArray = new String[result.size()][rowSize];
        for (int i = 0; i < returnArray.length; i++)
        {
            returnArray[i] = (String[])result.get(i);
        }
        return returnArray;
    }

    /**
     * 去掉字符串右边的空格
     * 
     * @param str 要处理的字符串
     * @return 处理后的字符串
     */
    private String rightTrim(String str)
    {
        if (str == null)
        {
            return "";
        }
        int length = str.length();
        for (int i = length - 1; i >= 0; i--)
        {
            if (str.charAt(i) != 0x20)
            {
                break;
            }
            length--;
        }
        return str.substring(0, length);
    }
    
    /** 
     * <Excel2007+文件的读取>
     * <Excel版本在2007以上时采用该方法读取，文件后缀为.xlsx>
     * @param file
     * @param ignoreRows
     * @return
     * @throws IOException
     * @see [类、类#方法、类#成员]
     */
    private String[][] getDataXlsx(String file,int ignoreRows) throws IOException
    {
        file = FILEPATH + File.separator + file;
        List<String[]> result = new ArrayList<String[]>();
        int rowSize = 0;
        XSSFWorkbook xwb = new XSSFWorkbook(file);
        // 循环读取各个章节(Sheet)中表格内容
        for (int sheetIndex = 0; sheetIndex < xwb.getNumberOfSheets(); sheetIndex++)
        {
            XSSFSheet sheet = xwb.getSheetAt(sheetIndex);
            // 定义 row、cell
            XSSFRow row = null;
            String cell = "";
            // 循环输出表格中的内容
            for (int rowIndex = sheet.getFirstRowNum()+ignoreRows; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++)
            {
                row = sheet.getRow(rowIndex);
                if (row == null)
                {
                    continue;
                }
                int tempRowSize = row.getLastCellNum() + 1;
                if (tempRowSize > rowSize)
                {
                    rowSize = tempRowSize;
                }
                String[] values = new String[rowSize];
                Arrays.fill(values, "");
                boolean hasValue = false;
                for (int cellIndex = row.getFirstCellNum(); cellIndex < rowSize/*row.getPhysicalNumberOfCells()*/; cellIndex++)
                {
                    // Excel中文本通过 row.getCell(j).toString() 获取单元格内容，数值则需要进行格式化
                    DecimalFormat df = new DecimalFormat("#");
                    try
                    {
                        if(null != row.getCell(cellIndex))
                        {
                            cell = String.valueOf(df.format(row.getCell(cellIndex).getNumericCellValue()));
                        }
                    }
                    catch (IllegalStateException e)
                    {
                        cell = row.getCell(cellIndex).toString();
                    }
                    /*if (cellIndex == 0 && cell.trim().equals(""))
                    {
                        break;
                    }*/
                    values[cellIndex] = rightTrim(cell);
                    hasValue = true;
                }
                if (hasValue)
                {
                    result.add(values);
                }
            }
        }
        String[][] returnArray = new String[result.size()][rowSize];
        for (int i = 0; i < returnArray.length; i++)
        {
            returnArray[i] = (String[])result.get(i);
        }
        return returnArray;
    }
	
	
	

	private boolean sendClaimMail(HashMap[] map, List<String> recipients)
			throws Exception {

		SimpleMailSender sms = MailSenderFactory
				.getSender(MailSenderType.SERVICE);
		StringBuilder mailSubject = new StringBuilder()
				.append("截止当前时间")
				.append(DateFormat.getInstance().format(
						Calendar.getInstance().getTime()))
				.append("5分钟内暂未接收到验证码的用户列表");
		StringBuilder mailContent = new StringBuilder();
		for (int i = 0; i < map.length; i++) {
			mailContent
					.append("手机号:")
					.append(map[i].get("phone_num"))
					.append(",")
					.append("验证码:")
					.append(map[i].get("verification_code"))
					.append(",")
					.append("错误信息:")
					.append(map[i].get("error_msg") == null ? "等待返回" : map[i]
							.get("error_msg")).append("</br>");
		}
		sms.send(recipients, mailSubject.toString(), mailContent.toString());
		return true;
	}

}
