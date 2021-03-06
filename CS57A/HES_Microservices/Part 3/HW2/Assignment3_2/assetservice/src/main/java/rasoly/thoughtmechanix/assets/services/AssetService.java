package rasoly.thoughtmechanix.assets.services;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import rasoly.thoughtmechanix.assets.clients.OrganizationRestTemplateClient;
import rasoly.thoughtmechanix.assets.config.ServiceConfig;
import rasoly.thoughtmechanix.assets.model.Asset;
import rasoly.thoughtmechanix.assets.model.Organization;
import rasoly.thoughtmechanix.assets.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    ServiceConfig config;

    @Autowired
    OrganizationRestTemplateClient organizationRestClient;


    public Asset getAsset(String organizationId, String assetId) {
        Asset asset = assetRepository.findByOrganizationIdAndAssetId(organizationId, assetId);

        Organization org = getOrganization(organizationId);

        return asset
                 .withOrganizationId(org.getId());
    }

    @HystrixCommand
    private Organization getOrganization(String organizationId) {
        return organizationRestClient.getOrganization(organizationId);
    }

    private void randomlyRunLong(){
      Random rand = new Random();

      int randomNum = rand.nextInt((3 - 1) + 1) + 1;

      if (randomNum==3) sleep();
    }

    private void sleep(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @HystrixCommand(fallbackMethod = "buildFallbackAssetList",
            threadPoolKey = "assetByOrgThreadPool",
            threadPoolProperties =
                    {@HystrixProperty(name = "coreSize",value="30"),
                     @HystrixProperty(name="maxQueueSize", value="10")},
            commandProperties={         
                     @HystrixProperty(name="circuitBreaker.requestVolumeThreshold", value="10"),
                     @HystrixProperty(name="circuitBreaker.errorThresholdPercentage", value="75"),
                     @HystrixProperty(name="circuitBreaker.sleepWindowInMilliseconds", value="7000"),
                     @HystrixProperty(name="metrics.rollingStats.timeInMilliseconds", value="15000"),
                     @HystrixProperty(name="metrics.rollingStats.numBuckets", value="5")}
    )
    public List<Asset> getAssetsByOrg(String organizationId){
        randomlyRunLong();

        return assetRepository.findByOrganizationId(organizationId);
    }

    private List<Asset> buildFallbackAssetList(String organizationId){
        List<Asset> fallbackList = new ArrayList<>();
        Asset asset = new Asset()
                .withId("0000000-00-00000")
                .withOrganizationId( organizationId )
                .withProductName("Sorry no asset information currently available");

        fallbackList.add(asset);
        return fallbackList;
    }

    public void saveAsset(Asset asset){
        asset.withId( UUID.randomUUID().toString());

        assetRepository.save(asset);
    }

    public void updateAsset(Asset asset){
      assetRepository.save(asset);
    }

    public void deleteAsset(Asset asset){
        assetRepository.delete( asset.getAssetId());
    }

}
