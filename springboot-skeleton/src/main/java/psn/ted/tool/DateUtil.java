package psn.ted.tool;

import static java.util.Objects.isNull;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;

public class DateUtil {

    public static final String DYNAMO_DATE_FEED_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String UI_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String UI_DATE_FORMAT = "yyyy-MM-dd";
    public static final String REPORTS_TIME_ZONE = "UTC+08:00";
    public static final String ORACLE_TIME_FORMAT_WITH_SEC = "HH:mm:ss";
    
    public static Timestamp current() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static Date today() {
        Date date = new Date(System.currentTimeMillis());
        return stripTime(date);
    }

    public static Date todayDateWithoutTime() throws ParseException {
        return today();
    }

    public static Date getLastDayOfCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        int lastDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        calendar.set(Calendar.DATE, lastDate);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);

        return calendar.getTime();
    }

    public static Date getFirstDayOfCurrentMonth() {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR, 00);
        calendar.set(Calendar.MINUTE, 00);
        calendar.set(Calendar.SECOND, 00);

        return calendar.getTime();
    }

    public static Date uiStringToDate(String uiValue) throws ParseException {
        return new SimpleDateFormat(UI_DATE_FORMAT).parse(uiValue);
    }

    public static DateTime dbStringToTime(String timeValue) {
        DateTime sometimeToday = DateTime.now();
        sometimeToday = sometimeToday.withHourOfDay(DateTime.parse(timeValue, DateTimeFormat.forPattern(ORACLE_TIME_FORMAT_WITH_SEC)).getHourOfDay());
        sometimeToday = sometimeToday.withMinuteOfHour(DateTime.parse(timeValue, DateTimeFormat.forPattern(ORACLE_TIME_FORMAT_WITH_SEC)).getMinuteOfHour());
        sometimeToday = sometimeToday.withSecondOfMinute(DateTime.parse(timeValue, DateTimeFormat.forPattern(ORACLE_TIME_FORMAT_WITH_SEC)).getSecondOfMinute());
        sometimeToday = sometimeToday.withMillisOfSecond(0);
        return sometimeToday;
    }

    public static DateTime appendDateWithTime(Date dateValue, DateTime timeValue) {
        DateTime someDateTime = new DateTime(dateValue);
        someDateTime = someDateTime.withHourOfDay(timeValue.getHourOfDay());
        someDateTime = someDateTime.withMinuteOfHour(timeValue.getMinuteOfHour());
        someDateTime = someDateTime.withSecondOfMinute(0);
        return someDateTime;
    }

    public static boolean isPast(Date dateInp) {
        return DateUtil.getDiffInDays(today(), stripTime(dateInp)) < 0;
    }

    public static int getDiffInMonths(Date nowDate, Date futureDate) {
        LocalDate nowDateLocal = stripDate(stripTime(nowDate)).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate futureDateLocal = stripDate(stripTime(futureDate)).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Period age = Period.between(nowDateLocal, futureDateLocal);
        return age.getYears() * 12 + age.getMonths();
    }

    public static boolean isSamePeriod(Date somePeriod, Date dateInp) {
        return getDiffInMonths(somePeriod, dateInp) == 0;
    }

    public static boolean isCurrentDay(Date dateInp) {
        return DateUtils.isSameDay(dateInp, today());
    }

    public static long getDiffInDays(Date nowDate, Date futureDate) {
        long duration = stripTime(futureDate).getTime() - stripTime(nowDate).getTime();
        return TimeUnit.MILLISECONDS.toDays(duration);
    }

    public static Date stripTime(Date someDate) {
        return DateUtils.truncate(someDate, Calendar.DATE);
    }

    public static Date stripDate(Date someDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(someDate);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR, 00);
        calendar.set(Calendar.MINUTE, 00);
        calendar.set(Calendar.SECOND, 00);
        return calendar.getTime();
    }

    /**
     * This method gets Seconds between Two Times
     *
     * @param nowDate
     * @param futureDate
     * @return
     */
    public static long getDiffInSeconds(Date nowDate, Date futureDate) {
        long duration = futureDate.getTime() - nowDate.getTime();
        return TimeUnit.MILLISECONDS.toSeconds(duration);
    }

    /**
     * This method gets Seconds between only Time Component INGNORE DATE
     *
     * @param nowDate
     * @param futureDate
     * @return
     */
    public static long getTimeDiffInSeconds(DateTime nowDate, DateTime futureDate) {
        if (isNull(nowDate) || isNull(futureDate))
            return -1;

        Seconds durationInSeconds = Seconds.secondsBetween(LocalTime.fromDateFields(nowDate.toDate()), LocalTime.fromDateFields(futureDate.toDate()));
        return durationInSeconds.getSeconds();
    }

    public static Date addDays(Date nowDate, int daysCount) {
        return DateUtils.addDays(nowDate, daysCount);
    }

    public static Date addMonths(Date nowDate, int monthCount) {
        return DateUtils.addMonths(nowDate, monthCount);
    }

    public static Boolean checkIfWeekend(Date dateInp) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateInp);
        int curr = cal.get(Calendar.DAY_OF_WEEK);
        return (curr == Calendar.SATURDAY) || (curr == Calendar.SUNDAY);
    }


    public static String ConvertDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = formatter.format(date);
        return formattedDate;
    }
    
    public static String ConvertDateWithTime(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	String formattedDate = formatter.format(date);
    	return formattedDate;
    }

    public static String convertDateTimeWithTimezone(Date date, String format) {
        if (date == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return ldt.atZone(ZoneId.systemDefault()).format(formatter);
    }
    public static String ConvertDateTimeToCST(LocalDateTime date) {
    	
    	if(date == null) {
    		return null;
    	}

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		return date.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of(REPORTS_TIME_ZONE)).toLocalDateTime().format(formatter); 			
    	
    }

    public static String ConvertDateToCST(LocalDateTime date) {
    	if(date == null) {
    		return null;
    	}

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return date.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of(REPORTS_TIME_ZONE)).toLocalDateTime().format(formatter);

    }
    
    public static String ConvertLocalDateTimeToUIFormat(LocalDateTime date) {
    	
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtil.UI_TIME_FORMAT);
    	return date.atZone(ZoneId.systemDefault()).format(formatter);
    	    	
    }

    public static Date ConverStringDateTime(String time) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat(DateUtil.TIMESTAMP_FORMAT);
        return formatter.parse(time);
    }

    public static void main(String[] args) {

        System.out.println(getDateTime(1526387453400l));
    }

    public static String getDateTime(long longTime) {
        SimpleDateFormat formatter = new SimpleDateFormat(DateUtil.TIMESTAMP_FORMAT);
        return dateToString(new Date(longTime), formatter);
    }

    public static String dateToString(Date date, SimpleDateFormat dateFormat) {
        return dateFormat.format(date);
    }

    public static long stringToLong(String time) throws ParseException{
        SimpleDateFormat formatter = new SimpleDateFormat(DateUtil.TIMESTAMP_FORMAT);
        Date date = formatter.parse(time);
        return date.getTime();
    }
}
