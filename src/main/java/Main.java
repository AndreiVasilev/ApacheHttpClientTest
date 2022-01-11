import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ConscryptClientTlsStrategy;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.ssl.SSLContexts;
import org.conscrypt.Conscrypt;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {
  public static void main(String[] args) throws ExecutionException, InterruptedException, NoSuchAlgorithmException, KeyManagementException {

    final Provider provider = Conscrypt.newProvider();
    Security.insertProviderAt(provider, 1);

    final SSLContext sslcontext = SSLContexts.custom()
        .setProvider(provider)
        .build();

    final PoolingAsyncClientConnectionManager manager = PoolingAsyncClientConnectionManagerBuilder.create()
        .setTlsStrategy(new ConscryptClientTlsStrategy(sslcontext))
        .build();

    final CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
        .setConnectionManager(manager)
        .build();
    httpclient.start();

    final SimpleHttpRequest request = SimpleRequestBuilder.get("https://www.amazon.com")
        .build();

    System.out.println("Executing request " + request);
    final Future<SimpleHttpResponse> future = httpclient.execute(
        request,
        new FutureCallback<SimpleHttpResponse>() {

          @Override
          public void completed(final SimpleHttpResponse response) {
            System.out.println(request + "->" + new StatusLine(response));
            System.out.println(response.getBody());
          }

          @Override
          public void failed(final Exception ex) {
            System.out.println(request + "->" + ex);
          }

          @Override
          public void cancelled() {
            System.out.println(request + " cancelled");
          }

        });
    future.get();

    System.out.println("Shutting down");
    httpclient.close(CloseMode.GRACEFUL);
  }
}
