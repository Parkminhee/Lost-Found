import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.gun0912.tedpicker.Config;
import com.gun0912.tedpicker.ImagePickerActivity;

import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class UploadActivity extends AppCompatActivity {
    private static String IP_ADDRESS = "192.168.219.106";
    private static String TAG = "phptest";

    String location="";
    double db_location1 = 0.0;
    double db_location2 = 0.0;

    String tempLoc = null;
    String selfcategory = "null";

    private int post_type=0; // 0:습득물 1:분실물

    //민희 09/27 추가
    ImageView imgview_1;
    ImageView imgview_2;
    ImageView imgview_3;

    EditText edit_title;
    EditText edit_etc;
    TextView text_date;
    TextView text_time;
    EditText edit_comment;
    EditText edit_take_place;

    Spinner mSpinner;
    ArrayAdapter mAdapter;
    int mYear, mMonth, mDay, mHour, mMinute;

    RadioGroup radioGroup;
    private RadioButton selectedRadio;
    private RadioButton radioButton;

    Geocoder geocoder;

    //민희 09/27 수정
    private static final int REQUEST_GET_IMAGES = 1;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    ArrayList<Uri> img_uris;
    ArrayList<String> img_names;

    //Firebase & SharedPreferences
    SharedPreferences sharedPreferences;
    private FirebaseAuth firebaseAuth;
    private String login_id = "null";
    private String uid = "null";

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        //챌
        sharedPreferences = getSharedPreferences("ID", Activity.MODE_PRIVATE);
        login_id = sharedPreferences.getString("login_id", "null");

        Log.i("minhee", "upload_create_1/login_id:" + login_id);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //민희 09/27 수정
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        img_uris = new ArrayList<Uri>();
        img_names = new ArrayList<String>();

        imgview_1 = (ImageView)findViewById(R.id.imgview_1);
        imgview_2 = (ImageView)findViewById(R.id.imgview_2);
        imgview_3 = (ImageView)findViewById(R.id.imgview_3);

        //유진
        edit_title = (EditText)findViewById(R.id.edit_title);
        edit_etc = (EditText)findViewById(R.id.post_category_etc);
        edit_take_place = (EditText)findViewById(R.id.edit_take_place);

        mAdapter = ArrayAdapter.createFromResource(this, R.array.category, android.R.layout.simple_spinner_dropdown_item);

        mSpinner = (Spinner)findViewById(R.id.post_category);
        mSpinner.setAdapter(mAdapter);

        //챌
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mSpinner.getSelectedItem().toString().equals("직접입력")){
                    edit_etc.setEnabled(true);
                } else {
                    edit_etc.setText("");
                    edit_etc.setEnabled(false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 
            }
        });

        text_date = (TextView)findViewById(R.id.text_date);
        text_time = (TextView)findViewById(R.id.text_time);

        Calendar cal = new GregorianCalendar();
        mYear = cal.get(Calendar.YEAR);
        mMonth = cal.get(Calendar.MONTH);
        mDay = cal.get(Calendar.DAY_OF_MONTH);
        mHour = cal.get(Calendar.HOUR_OF_DAY);
        mMinute = cal.get(Calendar.MINUTE);

        UpdateNow();

        edit_comment = (EditText)findViewById(R.id.edit_comment);
        geocoder = new Geocoder(this);
    }

    public void onClick (View v) {
        TextView textView;
        switch (v.getId()) {
            // 습득물 게시판 선택 시
            case R.id.btn_find_list: {
                textView = (TextView)findViewById(R.id.post_date);
                textView.setText("습득 날짜");
                textView = (TextView)findViewById(R.id.post_place);
                textView.setText("습득 위치");
                edit_take_place = (EditText)findViewById(R.id.edit_take_place);
                tempLoc = edit_take_place.getText().toString();
                edit_take_place.setEnabled(true);
                post_type=0;
                break;
            }
            // 분실물 게시판 선택 시
            case R.id.btn_lost_list: {
                textView = (TextView)findViewById(R.id.post_date);
                textView.setText("분실 날짜");
                textView = (TextView)findViewById(R.id.post_place);
                textView.setText("분실 위치");
                edit_take_place.setText("");
                edit_take_place.setEnabled(false);
                post_type=1;
                break;
            }
            case R.id.btn_select_photo: {
                // *** 갤러리에서 사진 선택 ***
                //민희 09/27 수정
                Config config = new Config();
                //config.setSelectionMin(1);
                config.setSelectionLimit(3);
                ImagePickerActivity.setConfig(config);

                Intent intent = new Intent(UploadActivity.this, ImagePickerActivity.class);
                startActivityForResult(intent, REQUEST_GET_IMAGES);

                break;
            }
            case R.id.btn_select_place_map: {
                // *** 맵 띄우기 ***
                textView = (TextView)findViewById(R.id.edit_place_search);
                textView.setEnabled(false);
                Intent intent = new Intent(this, PointLocationActivity.class);
                intent.putExtra("lat", 0);
                intent.putExtra("lon", 0);
                startActivityForResult(intent, 0);
                break;

            }
            case R.id.btn_select_place_search: {
                textView = (TextView)findViewById(R.id.edit_place_search);
                textView.setEnabled(true);
                break;
            }
            case R.id.btn_place_search: {
                // *** 검색하면 지도 띄우기 ***
                textView = (TextView)findViewById(R.id.edit_place_search);
                if (textView.isEnabled()) {
                    List<Address> list = null;

                    EditText et = (EditText) findViewById(R.id.edit_place_search);
                    String str = et.getText().toString();
                    try {
                        list = geocoder.getFromLocationName(
                                str, // 지역 이름
                                10); // 읽을 개수
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("test", "입출력 오류 - 서버에서 주소변환시 에러발생");
                    }

                    if (list != null) {
                        if (list.size() == 0) {
                            Toast.makeText(this, "해당되는 주소 정보는 없습니다", Toast.LENGTH_SHORT).show();
                        } else {
                            //EditText et2 = (EditText) findViewById(R.id.edit_comment);
                            //et2.setText(list.get(0).getLongitude() + ", " + list.get(0).getLatitude());
                            //          list.get(0).getCountryName();  // 국가명
                            //          list.get(0).getLatitude();        // 위도
                            //          list.get(0).getLongitude();    // 경도
                            Intent intent = new Intent(this, PointLocationActivity.class);
                            intent.putExtra("lat", (double)list.get(0).getLatitude());
                            intent.putExtra("lon", (double)list.get(0).getLongitude());
                            Log.i("주소 포스트", list.get(0).getLatitude()+""+list.get(0).getLongitude());
                            startActivityForResult(intent, 0);
                        }
                    } else {
                        Log.i("Tete", "리스트없다");
                    }
                }
                break;
            }
            // 날짜 및 시간 선택
            case R.id.btn_select_date: {
                new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay).show();
                break;
            }
            case R.id.btn_select_time: {
                new TimePickerDialog(this, mTimeSetListener, mHour, mMinute, false).show();
                break;
            }
            case R.id.text_date: {
                new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay).show();
                break;
            }
            case R.id.text_time: {
                new TimePickerDialog(this, mTimeSetListener, mHour, mMinute, false).show();
                break;
            }
            // 액티비티 종료
            case R.id.btn_post_cancel: {
                finish();
                break;
            }
            case R.id.btn_post_register:{ //채린
                String lorf = "f";
                String writer_id = null;
                String title = null;
                String category = null;
                String date = null;
                String time = null;
                String location1 = null;
                String location2 = null;
                String expln = null;
                String isfound = "0";

                //분실물인지 습득물인지
                radioGroup = (RadioGroup) findViewById(R.id.radio);
                int selectedRadioId = radioGroup.getCheckedRadioButtonId();
                selectedRadio = (RadioButton)findViewById(selectedRadioId);
                radioButton = (RadioButton)findViewById(R.id.btn_lost_list);

                if(selectedRadio == radioButton){
                    lorf = "l";
                    edit_take_place.setText("");
                }else{
                    lorf = "f";
                }

                //글쓴이 아이디
                writer_id = login_id;

                //글
                title = edit_title.getText().toString();

                //카테고리
                Spinner mySpinner = (Spinner) findViewById(R.id.post_category);
                String category_text = mySpinner.getSelectedItem().toString();
                if(category_text.equals("직접입력")){
                    selfcategory = edit_etc.getText().toString();
                    //직접입력이지만 기본 카테고리일 때
                    if(selfcategory.equals("지갑")) {
                        category = "0";
                        selfcategory = "null";
                    }else if(selfcategory.equals("휴대폰")||selfcategory.equals("핸드폰")){
                        category = "1";
                        selfcategory = "null";
                    }else if(selfcategory.equals("가방")){
                        category = "2";
                        selfcategory = "null";
                    }else if(selfcategory.equals("책")){
                        category = "3";
                        selfcategory = "null";
                    }else if(selfcategory.equals("의류")||selfcategory.equals("옷")){
                        category = "4";
                        selfcategory = "null";
                    }else if(selfcategory.equals("전자기기")||selfcategory.equals("노트북")){
                        category = "5";
                        selfcategory = "null";
                    }else if(selfcategory.equals("이어폰")||selfcategory.equals("충전기")||selfcategory.equals("에어팟")||selfcategory.equals("헤드폰")) {
                        category = "6";
                        selfcategory = "null";
                    }else if(selfcategory.equals("카드")||selfcategory.equals("신분증")||selfcategory.equals("민증")||selfcategory.equals("기숙사카드")){
                        category = "7";
                        selfcategory = "null";
                    }else if(selfcategory.equals("우산")){
                        category = "8";
                        selfcategory = "null";
                    }else if(selfcategory.equals("액세서리")||selfcategory.equals("악세사리")||selfcategory.equals("귀걸이")||selfcategory.equals("반지")||selfcategory.equals("목걸이")){
                        category = "9";
                        selfcategory = "null";
                    }else{
                        category = "10";
                        selfcategory = edit_etc.getText().toString();
                    }
                }else{
                    switch (category_text){
                        case "지갑":{
                            category = "0";
                            break;
                        }
                        case "휴대폰":{ //휴대폰으로 통일하기
                            category = "1";
                            break;
                        }
                        case "가방":{
                            category = "2";
                            break;
                        }
                        case "책":{
                            category = "3";
                            break;
                        }
                        case "의류":{
                            category = "4";
                            break;
                        }
                        case "전자기기":{
                            category = "5";
                            break;
                        }
                        case "이어폰·충전기":{
                            category = "6";
                            break;
                        }
                        case "카드·신분증":{
                            category = "7";
                            break;
                        }
                        case "우산":{
                            category = "8";
                            break;
                        }
                        case "액세서리":{
                            category = "9";
                            break;
                        }
                        default:{
                            category = "10";
                            break;
                        }
                    }
                }

                //날짜
                date = text_date.getText().toString();
                time = text_time.getText().toString();

                //위치
                location1 = String.valueOf(db_location2);
                location2 = String.valueOf(db_location1);
                Log.i(TAG, "위치알려줘"+location1+" , "+location2);

                //설명
                expln = edit_comment.getText().toString();
                tempLoc = edit_take_place.getText().toString();
                if(post_type==1){
                    Log.i(TAG, "맡긴위치없음");
                }else{
                    StringBuilder sb = new StringBuilder(expln);
                    sb.append("\n\n분실물을 맡긴 위치:");
                    sb.append(tempLoc);
                    expln = sb.toString();
                }
                Log.i(TAG, "explain:"+expln);

                //회수여부
                isfound = "0"; //기본값 0:못찾음

                if(title.isEmpty()){
                    Toast.makeText(this, "글 제목을 입력하세요", Toast.LENGTH_SHORT).show();
                }else if(category.isEmpty()){
                    Toast.makeText(this, "카테고리를 선택하세요", Toast.LENGTH_SHORT).show();
                }else if(category.equals("10")&&selfcategory.isEmpty()){
                    Toast.makeText(this, "카테고리를 선택하세요", Toast.LENGTH_SHORT).show();
                }else if(date.isEmpty()){
                    Toast.makeText(this, "날짜를 선택하세요", Toast.LENGTH_SHORT).show();
                }else if(time.isEmpty()) {
                    Toast.makeText(this, "시간를 선택하세요", Toast.LENGTH_SHORT).show();
                }else if(location1.equals("0.0")||location2.equals("0.0")){
                    Toast.makeText(this, "위치를 입력하세요", Toast.LENGTH_SHORT).show();
                }else if(expln.isEmpty()){
                    Toast.makeText(this, "분실물에 대한 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
                }else if(img_uris.isEmpty() && lorf.equals("f")) {  //민희 09/27 수정
                    Toast.makeText(this, "1개 이상의 이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
                } else{
                    String datetime = date+" "+time;
                    UploadPost task = new UploadPost();

                    //민희 09/27 수정
                    if(img_uris.isEmpty()) {
                        task.execute("http://" + IP_ADDRESS + "/post_test.php", lorf, writer_id, title, category, datetime, location1, location2, location, expln, isfound, selfcategory, "null", "null", "null");
                    } else if(img_uris.size() == 1) {
                        task.execute("http://" + IP_ADDRESS + "/post_test.php", lorf, writer_id, title, category, datetime, location1, location2, location, expln, isfound, selfcategory, img_names.get(0), "null", "null");
                    } else if(img_uris.size() == 2) {
                        task.execute("http://" + IP_ADDRESS + "/post_test.php", lorf, writer_id, title, category, datetime, location1, location2, location, expln, isfound, selfcategory, img_names.get(0), img_names.get(1), "null");
                    } else if(img_uris.size() == 3) {
                        task.execute("http://" + IP_ADDRESS + "/post_test.php", lorf, writer_id, title, category, datetime, location1, location2, location, expln, isfound, selfcategory, img_names.get(0), img_names.get(1), img_names.get(2));
                    }

                    Log.i("주소 변환", location); //location 값 php로 db에 넘기기
                    finish(); //글두개 연속으로 쓰면 오류
                }
                break;
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                // MainActivity 에서 요청할 때 보낸 요청 코드 (3000)
                case 0: {
                    TextView textView = (TextView) findViewById(R.id.text_locate);
                    db_location1 = data.getDoubleExtra("lon", 0);
                    db_location2 = data.getDoubleExtra("lat", 0);
                    textView.setText("주소 변환 중...");

                    MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(Double.valueOf(db_location2), Double.valueOf(db_location1));
                    MapReverseGeoCoder reverseGeoCoder = new MapReverseGeoCoder("7bbff578e4cf9f903fa39bf65aac39f4", mapPoint, new MapReverseGeoCoder.ReverseGeoCodingResultListener() {
                                @Override
                                public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
                                    Log.i("주소변환 성공 : ", s);
                                    location = s;
                                    TextView tv = (TextView)findViewById(R.id.text_locate);
                                    tv.setText(s);
                                }
                                @Override
                                public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
                                    Log.i("주소변환 실패 : ", "무척 안타깝네요");
                                }
                            }, this);
                    reverseGeoCoder.startFindingAddress();
                    break;
                }
                //민희 09/27 추가
                case 1: {
                    img_names.clear();
                    imgview_1.setImageResource(R.drawable.img_noimage);
                    imgview_2.setImageResource(R.drawable.img_noimage);
                    imgview_3.setImageResource(R.drawable.img_noimage);

                    img_uris = data.getParcelableArrayListExtra(ImagePickerActivity.EXTRA_IMAGE_URIS);

                    for (int i = 0; i < img_uris.size(); i++) {
                        String img_uriTxt = img_uris.get(i).toString();
                        img_names.add(img_uriTxt.substring(img_uriTxt.lastIndexOf("/")+1));
                    }

                    if(img_uris.size() == 0) {
                        imgview_1.setImageResource(R.drawable.img_noimage);
                        imgview_2.setImageResource(R.drawable.img_noimage);
                        imgview_3.setImageResource(R.drawable.img_noimage);
                    } else if(img_uris.size() == 1) {
                        imgview_1.setImageURI(img_uris.get(0));
                        imgview_2.setImageResource(R.drawable.img_noimage);
                        imgview_3.setImageResource(R.drawable.img_noimage);
                    } else if(img_uris.size() == 2) {
                        imgview_1.setImageURI(img_uris.get(0));
                        imgview_2.setImageURI(img_uris.get(1));
                        imgview_3.setImageResource(R.drawable.img_noimage);
                    } else if(img_uris.size() == 3) {
                        imgview_1.setImageURI(img_uris.get(0));
                        imgview_2.setImageURI(img_uris.get(1));
                        imgview_3.setImageURI(img_uris.get(2));
                    }
                    break;
                }
            }
        } else {
            switch (requestCode) {
                case 1: {
                    //Toast.makeText(this, "1개 이상의 이미지를 선택해주세요.", Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

    //민희
    class UploadPost extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(UploadActivity.this,"게시글 업로드 중...",null,true,true);
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            progressDialog.dismiss();
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = (String)params[0];
            String lorf = (String)params[1];
            String writer_id = (String)params[2];
            String title = (String)params[3];
            String category = (String)params[4];
            String datetime = (String)params[5];
            String location1 = (String)params[6];
            String location2 = (String)params[7];
            String stringlocation = (String)params[8];
            String expln = (String)params[9];
            String isfound = (String)params[10];

            String img1 = (String)params[12];
            String img2 = (String)params[13];
            String img3 = (String)params[14];

            try {
                DataOutputStream dataOutputStream = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;

                long now = System.currentTimeMillis();
                Date date = new Date(now);
                String curTime = simpleDateFormat.format(date);

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpURLConnection.setRequestProperty("ENCTYPE", "multipart/form-data");
                httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary + ";charset=utf-8");
                httpURLConnection.connect();

                DataOutputStream dos = new DataOutputStream(httpURLConnection.getOutputStream());

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"lorf\"\r\n\r\n" + lorf);
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"writer_id\"\r\n\r\n" + writer_id);
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
                dos.write(title.getBytes("UTF-8"));
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"category\"\r\n\r\n" + category);
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"datetime\"\r\n\r\n" + datetime);
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"location1\"\r\n\r\n" + location1);
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"location2\"\r\n\r\n" + location2);
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"stringlocation\"\r\n\r\n");
                dos.write(location.getBytes("UTF-8"));
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"expln\"\r\n\r\n");
                dos.write(expln.getBytes("UTF-8"));
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"isfound\"\r\n\r\n" + isfound);
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"selfcategory\"\r\n\r\n");
                dos.write(selfcategory.getBytes("UTF-8"));
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"curTime\"\r\n\r\n" + curTime.toString());
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                //이미지 1 시작
                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"img1\"\r\n\r\n" + img1);
                dos.writeBytes("\r\n--" + boundary + "\r\n");
                //이미지 1 끝

                //이미지 2 시작
                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"img2\"\r\n\r\n" + img2);
                dos.writeBytes("\r\n--" + boundary + "\r\n");
                //이미지 2 끝

                //이미지 3 시작
                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"img3\"\r\n\r\n" + img3);
                dos.writeBytes("\r\n--" + boundary + "\r\n");
                //이미지 3 끝

                dos.writeBytes("\r\n--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"imgCount\"\r\n\r\n" + img_names.size());
                dos.writeBytes("\r\n--" + boundary + "\r\n");

                if(img_names.size() > 0) {
                    for (int i = 0; i < img_names.size(); i++) {
                        String a = String.valueOf(i+1);

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4;   // 또는 4로 지정

                        File cacheDir = getCacheDir();
                        String tmpFileName = img_names.get(i);
                        File tmpFile = new File(cacheDir, tmpFileName);
                        tmpFile.createNewFile();
                        FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
                        Bitmap orgFile = BitmapFactory.decodeFile(img_uris.get(i).toString(), options);
                        orgFile.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);

                        //이미지파일 전송
                        File file = new File(cacheDir + "/" + tmpFileName);
                        FileInputStream fileInputStream = new FileInputStream(file);
                        //File file = new File(img_uris.get(i).toString());
                        //FileInputStream fileInputStream = new FileInputStream(file);

                        dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
                        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file_" + a + "\";filename=\"" + cacheDir + "/" + tmpFileName + "\"" + lineEnd);
                        dataOutputStream.writeBytes(lineEnd);

                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {
                            dataOutputStream.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        }

                        dataOutputStream.writeBytes(lineEnd);
                        dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    }
                }

                InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String result;

                while ((result = bufferedReader.readLine()) != null) {
                    stringBuilder.append(result + "\n");
                }

                Log.i("minhee", stringBuilder.toString());
                return stringBuilder.toString();
            } catch (Exception e) {
                Log.d(TAG, "InsertData: Error ", e);
                return e.toString();
            }
        }
    }


    // 날짜 & 시간 선택 리스너 등록
    DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            UpdateNow();
        }
    };
    TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mHour = hourOfDay;
            mMinute = minute;
            UpdateNow();
        }
    };

    // 날짜 업데이트
    void UpdateNow(){
        text_date.setText(String.format("%04d-%02d-%02d", mYear, mMonth + 1, mDay));
        text_time.setText(String.format("%02d:%02d", mHour, mMinute));
    }
}
