# CloudAnalyzer

CloudAnalyzer allows users to locally measure their individual cloud usage on their Android device.

## About

Developers of smartphone apps increasingly rely on cloud services for ready-made functionalities, e.g., to track app usage, to store data, or to integrate social networks.
At the same time, mobile apps have access to various private information, ranging from users’ contact lists to their precise locations.
As a result, app deployment models and data flows have become too complex and entangled for users to understand.
CloudAnalyzer, a transparency technology, that reveals the cloud usage of smartphone apps and hence provides users with the means to reclaim informational self-determination.

##### Features

* locally observe network traffic (through VPN interface)
* link observed traffic to installed applications
* show statistics about:
  * the amount of cloud traffic
  * the location the traffic flows to
  * the entanglement of different cloud services (and apps)
* help users to evaluate their (cloud) app usage

##### Privacy Statement

CloudAnalyzer does not track individual users.
Please refer to our [privacy policy](https://www.comsys.rwth-aachen.de/fileadmin/misc/trinics/PrivacyPolicy.html) for further information on which information CloudAnalyzer collects and how it is used.

##### Requirements

* Android 4.4 or later
* Granting of a limited set of Android permissions
  * ACCESS_NETWORK_STATE - for reacting to network changes
  * BIND_VPN_SERVICE - for setting up the emulated VPN (during runtime)
  * INTERNET - self-explanatory
  * RECEIVE_BOOT_COMPLETED - for auto-starting the application
  * WRITE_EXTERNAL_STORAGE - for data export (optional during runtime)

CloudAnalyzer does not require root permissions and therefore should work on all current Android devices.

##### Limitations

Android only supports the use of a single VPN service at the same time.
Hence, other VPNs cannot be used at the same time when CloudAnalyzer is running.

Currently, our VPN implementation fails to support IPv6.
Besides, we have reports about issues with STUN under certain situations.

The VPN interface operates with the pre-configured IP address 10.0.0.1 and the pre-configured DNS server 8.8.8.8.
If necessary, you can adjust these values to your needs.

## Contact

##### Download

The application is available at the following App stores:

* [Play Store](https://play.google.com/store/apps/details?id=de.rwth.comsys.cloudanalyzer)


##### Details

* Mail: cloudanalyzer (at) comsys (dot) rwth-aachen (dot) de
* Web: https://www.comsys.rwth-aachen.de/

##### Publications

The paper [CloudAnalyzer: Uncovering the Cloud Usage of Mobile Apps](https://www.comsys.rwth-aachen.de/fileadmin/papers/2017/2017-henze-mobiquitous-cloudanalyzer.pdf) contains results for a user study with 29 volunteers as well as superficial implementational details.
The paper [Towards Transparent Information on Individual Cloud Service Usage](https://www.comsys.rwth-aachen.de/fileadmin/papers/2016/2016-henze-cloudcom-trinics.pdf) mainly focuses on conceptual aspects of CloudAnalyzer.
If you use any portion of CloudAnalyzer in your work, please cite our publications.

BibTeX:
```
@inproceedings{cloudanalyzer-mobiquitous,
    author = {Henze, Martin and Pennekamp, Jan and Hellmanns, David and M{\"u}hmer, Erik and Ziegeldorf, Jan Henrik and Drichel, Arthur and Wehrle, Klaus},
    title = {{CloudAnalyzer: Uncovering the Cloud Usage of Mobile Apps}},
    booktitle = {Proceedings of the 14th EAI International Conference on Mobile and Ubiquitous Systems: Computing, Networking and Services (MobiQuitous)},
    month = {11},
    year = {2017},
    doi = {10.1145/3144457.3144471}
}

@inproceedings{trinics-cloudcom,
    author = {Henze, Martin and Kerpen, Daniel and Hiller, Jens and Eggert, Michael and Hellmanns, David and M{\"u}hmer, Erik and Renuli, Oussama and Maier, Henning and St{\"u}ble, Christian and H{\"a}u{\ss}ling, Roger and Wehrle, Klaus},
    title = {{Towards Transparent Information on Individual Cloud Service Usage}},
    booktitle = {2016 IEEE International Conference on Cloud Computing Technology and Science (CloudCom)},
    month = {12},
    year = {2016},
    doi = {10.1109/CloudCom.2016.0064}
}
```


## Setup

##### Installation

Simply download and install the application from the [Play Store](https://play.google.com/store/apps/details?id=de.rwth.comsys.cloudanalyzer).

If you want to build and setup CloudAnalyzer yourself, you require:

* [Android Studio](http://developer.android.com/sdk/)

The most important configuration parameters are stored in the [assets](./app/main/java/com/squareup/jnagmp/LibGmp.java) folder.

##### Usage

1. Accept the terms of use.
1. Start the "Live Analysis" and grant permission to the VPN interface.
1. Done.

Optionally, change the settings to exclude specific apps from the measurements or to opt-in to different comparison groups.

##### License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.

If you are planning to integrate CloudAnalyzer into a commercial product and do not want to disclose your source code, please contact us for other licensing options via email at cloudanalyzer (at) comsys (dot) rwth-aachen (dot) de

##### Dependencies

CloudAnalyzer relies on the following external components:

* [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
* [libsodium-jni](https://github.com/joshjdevl/libsodium-jni)
* [javallier](https://github.com/n1analytics/javallier)

All these dependencies are published under Apache 2.0 license.

We include the following JniLibs:
* [jna](https://github.com/java-native-access/jna) - extracted
* [gmp](https://github.com/Rupan/gmp) - copied
* [libsodium-jni](https://github.com/joshjdevl/libsodium-jni) - self-built

## Contributors

* Code: Martin Henze, Erik Mühmer, Jan Pennekamp, Ina Berenice Fink, David Hellmanns
* Detection patterns: Martin Henze, Jan Pennekamp, Erik Mühmer

CloudAnalyzer has been developed at the [Chair of Communication and Distributed Systems](https://www.comsys.rwth-aachen.de/) at [RWTH Aachen University](https://www.rwth-aachen.de/). This work has received funding from the German Federal Ministry of Education and Research (BMBF) under project funding reference no. 16KIS0351 (TRINICS). The resonsibility for the development and operation of the app lies with the  Chair of Communication and Distributed Systems at RWTH Aachen University.

---

## Appendix

##### Screenshots

<img src="https://raw.githubusercontent.com/COMSYS/CloudAnalyzer/master/screenshots/01_TermsOfUse.png" width="180" height="320" />
<img src="https://raw.githubusercontent.com/COMSYS/CloudAnalyzer/master/screenshots/04_StartUp_Screen.png" width="180" height="320" />
<img src="https://raw.githubusercontent.com/COMSYS/CloudAnalyzer/master/screenshots/05_Apps_View.png" width="180" height="320" />
<img src="https://raw.githubusercontent.com/COMSYS/CloudAnalyzer/master/screenshots/06_Apps_View_continued.png" width="180" height="320" />

<img src="https://raw.githubusercontent.com/COMSYS/CloudAnalyzer/master/screenshots/07_Services_View.png" width="180" height="320" />
<img src="https://raw.githubusercontent.com/COMSYS/CloudAnalyzer/master/screenshots/08_Services_View_continued.png" width="180" height="320" />
<img src="https://raw.githubusercontent.com/COMSYS/CloudAnalyzer/master/screenshots/09_Locations_View.png" width="180" height="320" />
<img src="https://raw.githubusercontent.com/COMSYS/CloudAnalyzer/master/screenshots/10_Locations_View_continued.png" width="180" height="320" />

For more screenshots, see [here](https://github.com/COMSYS/CloudAnalyzer/tree/master/screenshots).

##### FAQ

- What does this app do?
  This application analyzes your network traffic to generate statistics about used cloud services your devices connects to.
- How does this work?
  Android offers a VPN interface, thus, no root permission or external component (e.g., VPN server) is required for the analysis. The network traffic is matched against a list of known IP addresses, domain names, and certificates to determine which (if any) service a packet corresponds to.
  The network traffic is not being routed through an external VPN server.
- Which information is gathered?
  To establish a baseline, we measure the number of packets and their size for each app. We distinguish the statistics based on the following criteria: direction (incoming or outgoing?), importance (screen turned on or off?), link (cellular or Wi-Fi?), and protocol (encrypted or not?). Additionally, for matched packets we store which information source identified the cloud service (e.g., IP address, domain name, or certificate). In case we are able to identify which geographical region the packet is sent to, we also store this information.
- Can I verify that?
  Sure, you can simply export your database in the app and use an SQLite viewer to open your personal database. For example, the portable version of "DB Browser for sqlite" works just fine. The databse will then be located at /sdcard/Download/de.rwth.comsys.cloudanalyzer/caDb.
- What is the region number X?
  We have a custom mapping to identify different regions based on numbers. We can share this mapping upon request. The app, however, will show actual regions instead of numbers, so this might already be sufficient.
- What is app x.y.z?
  We identify applications based on the package name, which is supposed to be unique. In case you are unsure, which application matches this package name and the application is being distributed through the Play store, you can use the following link https://play.google.com/store/apps/details?id=x.y.z to get more information.
- I am not confident with sharing information about app X.
  You can specify which apps should not appear as individual entries in the database. We will still try to match all your traffic and accumulate the traffic to the baseline, however, it is impossible to draw any conclusions such as which app was responsible for the traffic. Filtering an application not only prevents future, additions, it also removes all existing data related to that application from the database.
- The notifications are annoying. Can I turn them off?
  Sure, you can customize which notifications are shown in the settings menu.

##### Cloud Services

Currently, CloudAnalyzer supports the detection of the following **90** cloud services (and their current subsidiaries).

* AdColony
* Adjust
* Adobe
* Akamai
* Alibaba
* Amazon
* Appboy
* Apple
* AppLovin
* Appnext
* AppNexus
* AppRiver
* AppsFlyer
* Apteligent
* Box
* CenturyLink
* Chartboost
* Cisco
* Cloudflare
* Cloudinary
* Comcast
* comScore
* Criteo
* Dell
* DimensionData
* Dropbox
* DuckDuckGo
* Epsilon
* Evernote
* ExperianMarketingServices
* Facebook
* Fastly
* Fronter
* Fujitsu
* GitHub
* GmoCloud
* GoDaddy
* Google
* imgur
* Incapsula
* Informatica
* InMobi
* Internap
* KeyCDN
* Kochava
* Leadbolt
* LinkedIn
* Localytics
* MAXMailProtection
* McAfee
* Microsoft
* Mimecast
* Mixpanel
* Netflix
* NTTCommunications
* Oracle
* Origin
* OVH
* Pinterest
* Proofpoint
* Rackspace
* RNTSMedia
* Salesforce
* Smaato
* Snap
* SoftLayer
* Sonobi
* SoundCloud
* sovrn
* Spotify
* StackPath
* StartApp
* Steam
* Strato
* StumbleUpon
* Supersonic
* Symantec
* Tapjoy
* TrendMicro
* Tune
* Twitter
* UnitedInternet
* UnityAds
* Verizon
* Vimeo
* VK
* VMware
* Vungle
* WeChat
* Yandex

