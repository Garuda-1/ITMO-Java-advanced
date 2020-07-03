# ITMO University advanced Java course solutions

## Course information

[Course website](http://www.kgeorgiy.info//courses/java-advanced/index.html)

**This website may not show all problem statements, specifically during spring term.**

For each problem students are provided with theoretical material, testing bundle and auxiliary classes or interfaces (in order to make solution comatible with tests). In order to accomplish each task, the solution had to pass both tests and code review. Mostly, the maximum number of code review requests was 3.

Each solution from this repository was considered valid and passed code review.

## Repository structure

  * ```artifacts```: All artifacts utilized by solutions.
  * ```java-solutions```: Module containing all solutions. Each solution is placed in an appropriate sub-package of ```ru.ifmo.rain.dolzhanskii``` package.
  * ```lib```: All libraries utilized by solutions.

## Compiling and running

In order to properly compile, the [teacher's repository](https://www.kgeorgiy.info/git/geo/java-advanced-2020) must be cloned in the same folder. However, some solutions (11 and 13) are completely independent from it as they required creation of custom tests.

**Teacher's repository may be deleted in near future in order not to disclose problem statements to future courses.** 

## Brief problem statements

### 1. I/O: **Walk**

Develop class ```RecursiveWalk```, calculating files hash in specified direcotry recursively.

* Console run format: ```java RecursiveWalk <input file> <output file>```.
* Input file contains list of directories needed to walk through.
* Output file must contains calculation results for each line of unput line in the following format: ```<hex hash> <path>```, one per line.
* Hash calculation algorithm: FNV.
* In case of any errors, the hash result must be ```00000000```.
* Input and output files encoding is UTF-8.
* Parent directory to the output file should be created if it is absent.
* File sizes may exceed RAM size.

Solution location: [```ru.ifmo.rain.dolzhanskii.walk```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/walk).

### 2. Collections Framework: **ArraySet**

Develop class ```ArraySet```, implementing immutable ordered set.

* Class ```ArraySet``` must implement ```NavigableSet``` interface.
* All methods must be maximally asymptotic effective.

Solution location: [```ru.ifmo.rain.dolzhanskii.arrayset```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/arrayset).

### 3. Data streams: **Students**

Develop class ```StudentDB```, processing search queries over students database.

* Class ```StudentDB``` must implement both ```StudentQuery``` and ```StudentGroupQuery``` interfaces.
* Each method must contains exactly one operator.

Solution location: [```ru.ifmo.rain.dolzhanskii.student```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/student).

### 4. Reflection: **Implementor**

Develop class ```Implementor```, generating basic class or interface implementation source code.

* Console arguments: qualified class or interface name to generate implementation of.
* The result of the run must be a generated java code with ```Impl``` suffix, extending (implementing) given class (interface).
* It must be possible to compile generated code without errors.
* Generated class cannot be abstract.
* Generated methods must ignore arguments and return default value of appropriate return type.
* Input classes or interfaces do not utilize generics.

Solution location: [```ru.ifmo.rain.dolzhanskii.implementor```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/implementor).
    
### 5. Jar: **Jar Implementor**

1. Create ```.jar``` file including compiled ```Implementor``` and all related classes.

    * Created archive must be runnable using command ```java -jar```
    * All additional command line arguments must be passed to ```Implementor```

2. Modify ```Implementor```. With additional argument ```-jar``` it must create ```.jar``` archive including generated implementation of given class or interface. 

3. It is also required to create following files.

    * Script to generate runnable ```.jar``` file including ```Implementor```.
    * Archive itself.

Solution location: [```ru.ifmo.rain.dolzhanskii.implementor```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/implementor).
    
### 6. Javadoc: **Implementor Javadoc**

1. Add comprehensive documentation to solutions of problems 4 and 5 using Javadoc.

    * All classes and all methods (including ```private``` ones) must be provided with documentation.
    * Documentation must be compilable without errors.
    * All links to standard library classes must be valid.

2. It is also required to create following files.

    * Script to generate the Javadoc.
    * Compiled Javadoc.

Solution location: [```ru.ifmo.rain.dolzhanskii.implementor```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/implementor).
    
### 7. Basic parallel computing: **Iterative parallelism**

Develop class ```IterativeParallelism```, processing lists in multiple threads.

* It is required to support following queries:

    * ```minimum(threads, list, comparator)```: first occurring minimum.
    * ```maximum(threads, list, comparator)```: first occurring maximum.
    * ```all(threads, list, predicate)```: check whether all elements satisfy given predicate.
    * ```any(threads, list, predicate)```: check whether any element satisfies given predicate.
    * ```filter(threads, list, predicate)```: list containing only mathcing given predicate elements.
    * ```map(threads, list, function)```: list containing results of function application to each element.
    * ```join(threads, list)```: concat string represenation of list elements.

* Number of threads created to compute the query must be equal to ```threads``` argument.
* Usage of Concurrency Utilities is prohibited.

Solution location: [```ru.ifmo.rain.dolzhanskii.concurrent```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/concurrent)
    
### 8. Basic parallel computing: **Parallel mapper*

1. Develop class ```ParallelMapperImpl```, implementing ```ParallelMapper``` interface.

    * Method ```run``` must concurrenlty compute given function on each element of ```args```.
    * Method ```close``` must terminate all threads.
    * Constructor ```ParallelMapperImpl(int threads)``` must create ```threads``` threads which may be utilized for future computations.
    * It is assumed that there may be several clients generating queries to compute.
    * All given tasks must be processed in queue.
    * There must not be any active standby.

2. Modify ```IterativeParallelism``` so it could utilize ```ParallelMapper```

    * Add constructor ```IterativeParallelism(ParallelMapper)```. If this constructor is used, this class is not allowed to create any additional threads.
    * It must be possible for several cients to utilize the same ```ParallelMapper```.

Solution location: [```ru.ifmo.rain.dolzhanskii.concurrent```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/concurrent)
    
### 9. Concurrency Utilities: **Web Crawler**

Develop thread-safe class ```WebCrawler``` implementing ```Crawler``` interface, recursively visiting web pages.

* The class must have the constructor, accepting the following arguments:

    * ```downloader```: ```Downloader``` instance to use in order to download pages and extract links.
    * ```downloaders```: Upper bound for number of pages concurrently downloading.
    * ```extractors```: Upper bound for number of pages concurrently analyzed for links extraction.
    * ```perHost```: Upper bound for number of pages concurrently downloading from the same host. 

* Upon ```download``` method call the class must recursively visit web pages, starting from the given initial URL. The search depth is limited by a given argument. This method must be thread-safe.
* Downloading and links extraction must be as much concurrent as possible, considering all passed in constructor limitations.
* It is allowed to use up to ```downloaders + extractors``` additional threads.
* It is prohibited to download page and extract links from it in a single thread.
* ```close``` method must terminate all created additional threads.
* Implementations of ```Downloader``` interface (provided in teacher's repository) must be utilized to download pages. 
* Implementations of ```Document``` interface (provided in teacher's repository) must be utilized to extract links.
* ```main``` method must implement console interface.

    * Console run format: ```WebCrawler url [depth [downloads [extractors [perHost]]]]
    * Used ```Downloader``` implementation: ```CachingDownloader```.

Solution location: [```ru.ifmo.rain.dolzhanskii.crawler```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/crawler)
        
### 10. Networking: **HelloUDP**
    
Develop client and server, interacting via UDP.

* Class ```HelloUDPClient``` must send requests to a designated server, receive responses and echo them to the console.

    * Command line arguments:
    
        * Server host name or IP.
        * Port which the server is listening to.
        * Request prefix.
        * Number of threads sending requests.
        * Number of requests per thread to send.
        
    * All requests must be sent concurrently in the given number of threads. Each thread must send its request, standby for response and echo both request and response. In case of no response, thread must try to send request again, terminating only upon reaching target number of requests for each thread.
    * Requests format: ```<request prefix><thread number>_<request number>```.
    
* Class ```HelloUDPServer``` must accept requests sent by ```HelloUDPClient``` and send appropriate response.

    * Command line arguments:
    
        * Port to listen.
        * Number of threads processing requests.
        
    * Response format: ```Hello, <request>```.
    * It is allowed to temporarily halt receiving new requests if server is busy.
    
Solution location: [```ru.ifmo.rain.dolzhanskii.hello```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/hello)
    
### 11. Serialization and RMI: **Bank**

**The initial bank implementation is located in course [examples of RMI usage](http://www.kgeorgiy.info//courses/java-advanced/examples/rmi.zip).**

* Add functionality to work with individuals (```Person```).

    * It must be possible to get first name, last name and passport number of given ```Person```.
    * Local ```Person``` instances (```LocalPerson```) must be transferred using serialization. These are snapshots and are not affected remotely since created.
    * Remote ```Person``` instances (```RemotePerson```) must be used via RMI. Only skeleton is transferred so all changes affect original ```Person``` stored in bank.
    * Each ```Person``` may possess several bank accounts those must be accessible. These accounts must have following ids: ```passport:subId```.
    * Bank functionality to add:
    
        * Lookup any instance type of ```Person``` by passport number.
        * Create new ```Person``` record.
        
* Develop demo application.

    * Command line arguments:
    
        * First name.
        * Last name.
        * Passport number.
        * Account ID.
        * Money amount to add.
        
    * If provided individual information is absent in bank, an appropriate ```Person``` must be created, otherwise given data is verified.
    * If account with given ID is not in individual's possession, the new one is created with initial amount of 0.
    * The designated account amount is increased by a given value. New amount must be printed to the console.
    
* Create tests verifying the foregoing behavior.

    * It is recommended to use JUnit 5.
    * Tests should not expect RMI Registry to be started preliminarily.
    * Create class ```BankTests``` which starts tests and prints results to the console.
    * Create script, launching ```BankTests```. Return code must be 0 only in case of success.
    * Create script, launching tests in a common way for the used testing framework. Return code must be 0 only in case of success.
    
Solution location: [```ru.ifmo.rain.dolzhanskii.bank```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/bank)
    
### 12. Non-blocking I/O: **HelloNonblockingUDP**

Develop client and server, interacting via UDP and using non-blocking I/O only.

* ```HelloUDPNonblockingClient``` class must have similiar functionality to ```HelloUDPClient```. It is prohibited to create additional threads.
* ```HelloUDPNonblockingServer``` class must have similiar functionality to ```HelloUDPServer```. All socket operations must be performed in a single thread.
* There should be no active standby.
    
Solution location: [```ru.ifmo.rain.dolzhanskii.hello```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/hello)

### 13. Internationalization and localization: **Text statistics**

* Develop ```TextStatistics``` application, analyzing languages in various languages.

    * Command line arguments: ```TextStatistics <text locale> <output locale> <text file> <report file>```
    * Supported text locales: all available locales in system.
    * Supported output locales: Russian, English.
    * Files encoding: UTF-8.
    * Categories to analyze:

        * Sentences.
        * Lines.
        * Words.
        * Numbers.
        * Money amounts.
        * Dates.

    * Statistcs to calculate by each category:

        * Number of occurrencies.
        * Number of distinct values.
        * Minimal value.
        * Maximal value.
        * Minimal length.
        * Maximal length.
        * Mean value / length.

    * Report format: HTML.
    * Texts to analyze size is expected to be less than RAM size.
    
* Create tests verifying the foregoing behavior.

Solution location: [```ru.ifmo.rain.dolzhanskii.i18n```](https://github.com/Garuda-1/java-advanced-2020-solutions/tree/master/java-solutions/ru/ifmo/rain/dolzhanskii/i18n)

## Disclaimer

This repository is public due to educational purposes. Note that it is under an appropriate license. You are not allowed to copy this code unless mentioning its author. 

**Warning to ITMO students currently or in future attending Java advanced course.**

* It is possible that your solutions will be tested for plagiarism. Copy of this repository is stored at university remote location making it possible to compare your code to this one.
* There are severe penalties for cheating, ranging from prohibiting you from getting any points for your solution to geting expelled from the university for plagiarism.
