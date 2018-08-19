# FindMyCar
Find your parked car instantly using your camera photo

<a href='https://play.google.com/store/apps/details?id=com.release.rroycsdev.findmycar&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' width="200" height="75" src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>

FindMyCar is a lightweight android application to help you find your parked car using a photo's geo-tag and Google APIs.
Unlike most other alternatives on the Google Play Store, you do not have to open the app to record your car's location.
Instead, just open your phone's camera, take a quick picture, and go on your way. Open the app only when you are ready to find your car.

FindMyCar utilizes MVVP architectural design pattern, Dagger 2, and Google APIs

There are two modes to find your car's location
# Last Image
Using your last taken camera photo, FindMyCar will instantly get the location of the photo and place a marker on the map.
You can choose either Walking or Driving mode to give you waypoints from your location to the photo's location. You also get a helpful toast message showing the distance and duration in the respective mode between the two locations. Additional, you can tap on the marker to open Google Maps with the designated points.

# Camera 
Using your previous <predefined number> camera photos, FindMyCar utilizes Text Recognition API (part of Google Mobile  Vision API)
to recognize and retrieve text from images. Using these text and <predefined text>, images with similar matching text are found and the geo-tag is extracted. The images and their location are mapped on Google Maps.
  
To provide quick text dectection using Text Recognition API, load balancing techniques using muilti-threads and lock mechanisms are applied. For instance, running 10 consecutive Text Recognition API calls take an average of 32 seconds as opposed to 6 seconds when load balancing is applied, an average runtime improvement of 81.25%.
