package com.example.qrcodescanner;

import static android.view.View.VISIBLE;
import static com.example.qrcodescanner.R.layout.activity_analysis;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
//for safety score
import java.util.concurrent.atomic.AtomicInteger;
//to access net
import android.net.Uri;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import okhttp3.*;
import android.app.AlertDialog;





public class Analysis extends AppCompatActivity {

    //private final OkHttpClient client = new OkHttpClient();

    //start with a score of 0
    private final AtomicInteger safetyScore = new AtomicInteger(0); // Start with a base score of 0



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_analysis);

        // Get the MyApp object
        MyApp myApp = (MyApp) getApplicationContext();

        // Get the URL from the MyApp object
        final String[] url = {myApp.getUrl()};

        //button for scan next
        Button scanNext = findViewById(R.id.scanNextButton);
        scanNext.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Call once you redirect to another activity
        });

        //button for visiting website
        Button goWebsite = findViewById(R.id.websiteButton);
        goWebsite.setOnClickListener(v -> {
            String url1 = myApp.getUrl();
            String finalUrl = myApp.getUrl();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url1));
            startActivity(intent);
        });


        ImageButton content = findViewById(R.id.contentInfo);
        content.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("X-header Content")
                    .setMessage("The X-Content-Type-Options HTTP header is a security feature that helps to protect a site from attacks such as drive-by downloads and from serving in an unexpected manner.\n" +
                            "\n" +
                            "This header has only one directive: nosniff. When X-Content-Type-Options: nosniff is set, " +
                            "the browser will strictly follow the MIME types returned in the Content-Type headers. " +
                            "It will not try to guess the MIME type if the server did not provide one, " +
                            "nor will it use the MIME type suggested by the client. This prevents \"mime\" based attacks.")
                    .setPositiveButton("close", (dialog, which) -> {
                        // do something when user press OK
                    })
                    .show();
        });

        ImageButton frame = findViewById(R.id.frameInfo);
        frame.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("X-frame option")
                    .setMessage("X-Frame-Options allows content publishers to prevent their own content from being used in an invisible frame by attackers. " +
                            "\n The DENY option is the most secure, preventing any use of the current page in a frame. More commonly, SAMEORIGIN is used, " +
                            "as it does enable the use of frames, but limits them to the current domain.")
                    .setPositiveButton("close", (dialog, which) -> {
                        // do something when user press OK
                    })
                    .show();
        });

        ImageButton policy = findViewById(R.id.policyInfo);
        policy.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Content Secure Policy")
                    .setMessage("A Content Protection Policy (CSP) is a security standard that provides an additional layer of protection from cross-site scripting (XSS), " +
                            "clickjacking, and other code injection attacks. " +
                            "\nIt is a defensive measure against any attacks that rely on executing malicious content in a trusted web context, " +
                            "or other attempts to circumvent the same-origin policy.")
                    .setPositiveButton("close", (dialog, which) -> {
                        // do something when user press OK
                    })
                    .show();
        });

        ImageButton xss = findViewById(R.id.xssInfo);
        xss.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("X-XSS Security")
                    .setMessage("The HTTP X-XSS-Protection response header is a feature of Internet Explorer, " +
                            "Chrome and Safari that stops pages from loading when they detect reflected cross-site scripting (XSS) attacks.")
                    .setPositiveButton("close", (dialog, which) -> {
                        // do something when user press OK
                    })
                    .show();
        });

        ImageButton transport = findViewById(R.id.transportInfo);
        transport.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Strict Transport Policy")
                    .setMessage("HTTP Strict Transport Security (HSTS) is a simple and widely supported standard to protect visitors by " +
                            "ensuring that their browsers always connect to a website over HTTPS. HSTS exists to remove the need for the common, " +
                            "insecure practice of redirecting users from http:// to https:// URLs.")
                    .setPositiveButton("close", (dialog, which) -> {
                        // do something when user press OK
                    })
                    .show();
        });


        ImageButton blackInfo = findViewById(R.id.blackInfo);
        blackInfo.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Blacklisted domain")
                    .setMessage("A domain that has been blacklisted is typically considered bad because it has been identified as engaging in " +
                            "activities that are fraudulent, harmful, or against the policy of the entity that created the blacklist.")
                    .setPositiveButton("close", (dialog, which) -> {
                        // do something when user press OK
                    })
                    .show();
        });

        ImageButton download = findViewById(R.id.downInfo);
        download.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Search for file download")
                    .setMessage("File Download is not a bad indicator for a website. " +
                            "\n However, it is still unsafe if a website downloads files into your devices without prior notice, as the " +
                            "downloaded files might contains malware, phishing content and trojan viruses that might causes harm to you.")
                    .setPositiveButton("close", (dialog, which) -> {
                        // do something when user press OK
                    })
                    .show();
        });




        // Execute redirection check in a separate thread
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(() -> {

            //set TextView
            TextView tvResult = findViewById(R.id.Result);
            TextView blacklisted = findViewById(R.id.blackR);
            TextView blackScore = findViewById(R.id.blackScore);
            TextView lasturl = findViewById(R.id.finalURLR);
            TextView download1 = findViewById(R.id.downR);
            TextView downScore = findViewById(R.id.downScore);
            //Image View
            ImageView tickC = findViewById(R.id.tickC);
            ImageView tickD = findViewById(R.id.tickD);


            //set all the image into untick
            tickC.setImageResource(R.drawable.untick1);
            tickD.setImageResource(R.drawable.untick1);

            //make sure that the link is not case sensitive
            url[0] = url[0].toLowerCase();

            //checking redirection
            boolean isRedirect = doesUrlRedirect(url[0]);
            //request final url
            //add redirection stop from clicking
            String finalurl = getFinalUrl(url[0]);

            //checking blacklist
            boolean isBlacklisted = checkUrlInBlacklist(url[0]);
            //check download
            boolean askDownload = promptsDownload(url[0]);

            fetchHttpHeaders(url[0]);



            handler.post(() -> {

                tvResult.setText(url[0]);
                lasturl.setText(finalurl);
                if(isRedirect){
                    url[0] = finalurl;
                }


                if(askDownload){
                    download1.setText("Yes");
                    downScore.setText("-10");
                    safetyScore.addAndGet(-10);
                }else{
                    download1.setText("No");
                    downScore.setText("0");
                }

                tickD.setImageResource(R.drawable.tick1);

                if (isBlacklisted) {
                    blacklisted.setText("Yes");
                    blackScore.setText("clear");
                    safetyScore.set(00);
                } else {
                    blacklisted.setText("No");
                    blackScore.setText("+20");
                    safetyScore.addAndGet(20);
                }

                //indicate that the checking complete
                tickC.setImageResource(R.drawable.tick1);



            });
        });
    }

    public boolean doesUrlRedirect(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // no data is needed
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(false); // Make the logic below easier to detect redirections
            conn.setRequestMethod("GET");

            // Get response code
            int responseCode = conn.getResponseCode();

            // Check if it's a redirection
            return ((responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                    || responseCode == 307  // Temporary Redirect
                    || responseCode == 308));  // Permanent Redirect
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getFinalUrl(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false); // Do not automatically follow redirects
            conn.connect(); // Connect to the URL

            // Check if the response code is a redirect (300-399)
            int responseCode = conn.getResponseCode();
            if((responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                    || responseCode == 307  // Temporary Redirect
                    || responseCode == 308)) {
                // This URL is a redirect. The "Location" header usually contains the new URL
                String redirectedUrl = conn.getHeaderField("Location");
                return getFinalUrl(redirectedUrl); // Recursive call to follow the redirection
            } else {
                // This URL is not a redirect
                return url;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean checkUrlInBlacklist(String urlToCheck) {
        try {
            URL url = new URL("https://github.com/stamparm/blackbook/blob/master/blackbook.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String blacklist = null;
            blacklist = reader.readLine();
            reader.close();
            return blacklist.contains(urlToCheck);
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean promptsDownload(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.connect(); // Connect to the URL

            // Get the Content-Disposition header from the response
            String contentDisposition = conn.getHeaderField("Content-Disposition");

            return contentDisposition != null && contentDisposition.startsWith("attachment");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void fetchHttpHeaders(String url) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .head() // Use HEAD to get only the headers, not the body
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {

                    //Textview
                    TextView recText = findViewById(R.id.recText);

                    //Imageview
                    ImageView recImage = findViewById(R.id.recImage);
                    TextView blacklisted = findViewById(R.id.blackR);
                    TextView blackScore = findViewById(R.id.blackScore);

                    Toast.makeText(Analysis.this, "Failed on header analysis", Toast.LENGTH_SHORT).show();


                });
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                //Textview
                TextView recText = findViewById(R.id.recText);
                TextView contentType = findViewById(R.id.contentType);
                TextView contentScore = findViewById(R.id.contentTypeScore);
                TextView frameType = findViewById(R.id.frameType);
                TextView frameScore = findViewById(R.id.frameScore);
                TextView policyType = findViewById(R.id.policyType);
                TextView policyScore = findViewById(R.id.policyScore);
                TextView xssType = findViewById(R.id.xxsType);
                TextView xssScore = findViewById(R.id.xxsScore);
                TextView transportType = findViewById(R.id.transportType);
                TextView transportScore = findViewById(R.id.transportScore);
                TextView score = findViewById(R.id.scoreR);

                //Imageview
                ImageView recImage = findViewById(R.id.recImage);
                ImageView contentTick = findViewById(R.id.tick1);
                ImageView frameTick = findViewById(R.id.tick2);
                ImageView policyTick = findViewById(R.id.tick3);
                ImageView xssTick = findViewById(R.id.tick4);
                ImageView transportTick = findViewById(R.id.tick5);

                Button goWebsite = findViewById(R.id.websiteButton);


                Headers headers = response.headers();
                String x_content_type_options = headers.get("X-Content-Type-Options");
                String x_frame_options = headers.get("X-Frame-Options");
                String content_security_policy = headers.get("Content-Security-Policy");
                String x_xss_protection = headers.get("X-XSS-Protection");
                String strict_transport_security = headers.get("strict-transport-security");

                runOnUiThread(() -> {

                    if (x_content_type_options != null && x_content_type_options.equals("nosniff")) {
                        // The X-Content-Type-Options header is safe

                        safetyScore.addAndGet(20);
                        contentType.setText("nosniff");
                        contentScore.setText("+20");
                        contentTick.setImageResource(R.drawable.tick1);


                    } else {
                        // The X-Content-Type-Options header is not safe
                        contentType.setText("null");
                        contentScore.setText("0");
                        contentTick.setImageResource(R.drawable.tick1);
                    }

                    if (x_frame_options == null) {
                        frameType.setText("DENY");
                        frameScore.setText("+15");
                        safetyScore.addAndGet(15);
                        frameTick.setImageResource(R.drawable.tick1);
                    }else{

                        switch (x_frame_options){

                            case "SAME_ORIGIN":
                                frameType.setText("same origin");
                                frameScore.setText("+15");
                                safetyScore.addAndGet(15);
                                frameTick.setImageResource(R.drawable.tick1);
                                break;

                            case "ALLOW_FROM":
                                frameType.setText("allow from");
                                frameScore.setText("0");
                                safetyScore.addAndGet(0);
                                frameTick.setImageResource(R.drawable.tick1);
                                break;

                            default:
                                frameType.setText("undetected");
                                frameScore.setText("0");
                                safetyScore.addAndGet(0);
                                frameTick.setImageResource(R.drawable.tick1);
                                break;

                        }
                    }



                    if (content_security_policy == null || content_security_policy.isEmpty()) {
                        policyType.setText("empty");
                        policyScore.setText("0");
                        safetyScore.addAndGet(0);
                        policyTick.setImageResource(R.drawable.tick1);

                    } else {
                        if (content_security_policy.contains("unsafe-inline")) {
                            policyType.setText("unsafe inline");
                            policyScore.setText("0");
                            safetyScore.addAndGet(0);
                            policyTick.setImageResource(R.drawable.tick1);
                        }

                        if (content_security_policy.contains("'unsafe-eval'")) {
                            policyType.setText("unsafe-eval");
                            policyScore.setText("0");
                            safetyScore.addAndGet(0);
                            policyTick.setImageResource(R.drawable.tick1);

                        } else{
                            policyType.setText("safe");
                            policyScore.setText("+15");
                            safetyScore.addAndGet(15);
                            policyTick.setImageResource(R.drawable.tick1);}


                    }

                    if (x_xss_protection == null) {
                        xssType.setText("null");
                        xssScore.setText("0");
                        safetyScore.addAndGet(0);
                        xssTick.setImageResource(R.drawable.tick1);
                    }else{
                        switch (x_xss_protection) {
                            case "0":
                                xssType.setText("0");
                                xssScore.setText("0");
                                safetyScore.addAndGet(0);
                                xssTick.setImageResource(R.drawable.tick1);
                                break;
                            case "1":
                            case "1; mode=block":
                                xssType.setText("1");
                                xssScore.setText("+15");
                                safetyScore.addAndGet(15);
                                xssTick.setImageResource(R.drawable.tick1);
                                break;

                            default:
                                xssType.setText("undetected");
                                xssScore.setText("0");
                                safetyScore.addAndGet(0);
                                xssTick.setImageResource(R.drawable.tick1);
                                break;
                        }
                    }

                        if ( strict_transport_security == null) {
                            transportType.setText("empty");
                            transportScore.setText("0");
                            safetyScore.addAndGet(0);
                            transportTick.setImageResource(R.drawable.tick1);
                        }else{
                            boolean includesSubdomains = strict_transport_security.contains("includeSubDomains");
                            boolean preload = strict_transport_security.contains("preload");

                            if (includesSubdomains || preload) {
                                transportType.setText("valid");
                                transportScore.setText("+15");
                                safetyScore.addAndGet(15);
                                transportTick.setImageResource(R.drawable.tick1);
                            }else if (includesSubdomains) {
                                transportType.setText("valid");
                                transportScore.setText("+25");
                                safetyScore.addAndGet(25);
                                transportTick.setImageResource(R.drawable.tick1);
                            } else {
                                transportType.setText("valid");
                                transportScore.setText("+25");
                                safetyScore.addAndGet(25);
                                transportTick.setImageResource(R.drawable.tick1);
                            }
                        }

                    int calc = safetyScore.get();
                        String calcS = Integer.toString(calc);
                    score.setText(calcS);

                    if(calc < 50) {
                        recImage.setImageResource(R.drawable.danger);
                        recText.setText("Mallicious Link");
                        recText.setVisibility(VISIBLE);
                        recImage.setVisibility(VISIBLE);
                        //goWebsite.setBackgroundColor(android.R.color.holo_red_dark);

                    } else if(calc >= 50 && calc < 70) {
                        recImage.setImageResource(R.drawable.suspicious);
                        recText.setText("Suspicious Link");
                        recText.setVisibility(VISIBLE);
                        recImage.setVisibility(VISIBLE);
                        //goWebsite.setBackgroundColor(android.R.color.yellow);

                    } else if(calc >= 70) {
                        recImage.setImageResource(R.drawable.safe);
                        recText.setText("Safe Link");
                        recText.setVisibility(VISIBLE);
                        recImage.setVisibility(VISIBLE);
                        //goWebsite.setBackgroundColor(getColor(green));
                    }


                });


            }
        });
    }

}
