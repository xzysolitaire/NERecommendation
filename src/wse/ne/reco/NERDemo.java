package wse.ne.reco;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** This is a demo of calling CRFClassifier programmatically.
 *  <p>
 *  Usage: {@code java -mx400m -cp "stanford-ner.jar:." NERDemo [serializedClassifier [fileName]] }
 *  <p>
 *  If arguments aren't specified, they default to
 *  classifiers/english.all.3class.distsim.crf.ser.gz and some hardcoded sample text.
 *  <p>
 *  To use CRFClassifier from the command line:
 *  </p><blockquote>
 *  {@code java -mx400m edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier [classifier] -textFile [file] }
 *  </blockquote><p>
 *  Or if the file is already tokenized and one word per line, perhaps in
 *  a tab-separated value format with extra columns for part-of-speech tag,
 *  etc., use the version below (note the 's' instead of the 'x'):
 *  </p><blockquote>
 *  {@code java -mx400m edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier [classifier] -testFile [file] }
 *  </blockquote>
 *
 *  @author Jenny Finkel
 *  @author Christopher Manning
 */

public class NERDemo {

  public static void main(String[] args) throws Exception {

    String serializedClassifier = "C://Users/acer/workspace/stanford-ner-2014-06-16/classifiers/english.conll.4class.distsim.crf.ser.gz";

    if (args.length > 0) {
      serializedClassifier = args[0];
    }

    AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);

    /* For either a file to annotate or for the hardcoded text example,
       this demo file shows two ways to process the output, for teaching
       purposes.  For the file, it shows both how to run NER on a String
       and how to run it on a whole file.  For the hard-coded String,
       it shows how to run it on a single sentence, and how to do this
       and produce an inline XML output format.
    */
    if (args.length > 1) {
      String fileContents = IOUtils.slurpFile(args[1]);
      List<List<CoreLabel>> out = classifier.classify(fileContents);
      for (List<CoreLabel> sentence : out) {
        for (CoreLabel word : sentence) {
          System.out.print(word.word() + '/' + word.get(CoreAnnotations.AnswerAnnotation.class) + ' ');
        }
        System.out.println();
      }
      System.out.println("---");
      out = classifier.classifyFile(args[1]);
      for (List<CoreLabel> sentence : out) {
        for (CoreLabel word : sentence) {
          System.out.print(word.word() + '/' + word.get(CoreAnnotations.AnswerAnnotation.class) + ' ');
        }
        System.out.println();
      }

    } else {
      String[] example = {"Al Horford welcomes new coach Mike Budenholzer and Paul Millsap to the Hawks' re-feathered nest. What's in store for the Atlanta Hawks? Our panel of five looks back at the offseason moves (and nonmoves) and forward to what lies ahead in the 2013-14 NBA season. 1. What grade would you give the Hawks' offseason? Bo Churney, HawksHoop: B+. Sure, the Hawks missed out on Dwight Howard and Chris Paul, but they still made a series of good moves. Paul Millsap will be a welcome replacement for poor Josh Smith's shooting habits, and Jeff Teague and Kyle Korver were brought back on manageable contracts. The Hawks didn't really get any better, but they didn't get worse, and they have much more cap flexibility than in the past. Amin Elhassan, ESPN Insider: C+. The Hawks had a solid offseason: They drafted well, avoided overpaying Smith, matched a reasonable offer sheet on Teague and picked up Millsap, Elton Brand and Gustavo Ayon on short-term, low-risk deals. I thought they overpaid for Korver, but they could afford to do that with the rest of their finances in order. Buddy Grizzard, HawksHoop: B-. Hawks GM Danny Ferry gets an A for signing Millsap to an instantly movable contract, but Al Horford's dissatisfaction with the state of the roster is alarming. With DeMarcus Cousins signing for a reported $62 million, and with Ferry shipping out Smith and Joe Johnson without returning equal talent, Horford must be wondering why he gave the Hawks a discount. Andrew Han, ClipperBlog: A-. The Hawks were never serious contenders to land a premier free agent this past offseason, but they did maximize everything that was in their control. Atlanta retained Teague and Korver, drafted incredibly intriguing prospects in Dennis Schroder and Lucas Nogueira and signed Millsap to one of the best value contracts in the league. Brian Robb, Celtics Hub: B+. Ferry avoided overpaying for Smith, got great value by bringing in an undervalued Millsap on a cheap deal, and maintained his long-term flexibility by signing key role players to reasonable contracts. Throw in a respected hire from the Gregg Popovich coaching tree, and there's little not to like about Atlanta's offseason. 2. What's the biggest question facing the Hawks in 2013-14? Player Profiles: Atlanta Hawks Check out scouting reports and projections for every player on the Atlanta Hawks roster. Profiles | Forecast Churney: When will Lou Williams come back from his ACL surgery and how effective will he be? The Hawks need someone that can go out by himself and get buckets. At full strength, Williams is definitely a guy that comes in looking to score, but if his injury holds him back too much the Hawks could find themselves in a bind when their pass-heavy offense is having trouble putting points on the board. Elhassan: \"What's really changed?\" The Hawks had a solid offseason, but in the grand scheme of things, they are pretty much in the same position they were in before: a middling team for the foreseeable future. Grizzard: Who is going to guard LeBron James and Carmelo Anthony? DeMarre Carroll looks like a value add, but he's the only small forward Ferry has signed (aside from camp invites) during his tenure. What happens when he's in foul trouble? Korver showed his inadequacy to guard elite wings against Paul George in last season's first-round series against the Pacers. Han: Quo vadimus? Where are the Hawks going? Are they maintaining a smart roster for a lower seed in the East, collect their late teens draft slot and build methodically, or will they trade in their talent for prospects and accrue assets? This is one of the few teams that can go either way; just a matter of preference. Robb: How will they fare without Smith? After letting the lifelong Hawk jump ship to the Pistons, I'm curious to see the effect his departure will have on Atlanta this season. His defensive prowess will be missed undoubtedly, but the Hawks losing his questionable shot selection from their offensive repertoire could produce a net positive for the team overall. 3. Who's the Hawks' most intriguing player? Churney: Schroder. The young German provides the Hawks with an intense type of defense that can completely change the pace of a game. Depending on how well the rookie can adapt to the NBA, we could see Teague on the trading block as early as this coming spring. Elhassan: Horford. He's a high IQ, skilled big who can score inside and out, a legitimate double-double threat every night out and on a steal of a contract. Horford might be the most underrated player in the NBA, but he's stuck on a very average team in Atlanta, and one has to wonder how many more years of \"average\" he can take before wanting out. Grizzard: Ayon, from a talent standpoint, is a starting-caliber NBA center. The issue for Ayon, as with Zaza Pachulia before him, is durability. Horford wants a center to play next to. Ayon has a refined post game and handles the ball like a guard. Unfortunately, he's not stout enough to make a defensive impact against the league's most bruising post players. Han: Millsap. A contender-level complementary player on a friendly contract, Millsap's performance could influence the building course of the franchise. If another team feels it is one piece away from competing for a title, it might be in the Hawks' best interests to get future value from Millsap rather than his current production. Robb: Millsap. After battling a crowded Utah frontcourt for both minutes and touches on the offensive end, Millsap should get more of a leading role in his new home. Is he capable of carrying Atlanta's offense with a nice set of complementary scoring weapons surrounding him? I'm eager to find out. 4. What's one bold prediction about the Hawks? More from ESPN.com Find out where our ESPN Forecast panel ranked each Hawk. #NBArank » Churney: They will finish above the Knicks in the East, which might say more about how I feel about New York than Atlanta. Carmelo is definitely the best player between these two squads, but it really seems that the pieces the Hawks have fit better than what the Knicks are putting out there. Elhassan: They'll be in the mix for the fourth seed until late in the season. That's as bold as it gets for this team this season! Grizzard: Schroder will be in the Rookie of the Year conversation. The GM who found Danny Green in the second round and the coach who pushed for the Kawhi Leonard trade have done it again. Teague will help Schroder develop without pressure, which will allow him to produce immediate results. The Hawks are desperate for the perimeter defense he provides. Han: Teague, Horford or Millsap will be traded during the season for an abundance of assets. Ferry's been deconstructing Atlanta's \"Treadmill of Mediocrity\" ever since his arrival last offseason and letting Smith leave without a fight is just another indicator that business is not as usual. Robb: Schroder will be one of the top rookies in the league. The 20-year-old guard enters the NBA with plenty of potential, but he should also get lots of opportunity right out of the gate with Lou Williams still recovering from a torn ACL. Look for him to provide a spark, especially on the defensive end, off Atlanta's bench every night. 5. Prediction time: How far will the Hawks go this season? Churney: They'll make the playoffs, but that's about it. The top four teams in the East (Miami, Brooklyn, Chicago and Indiana) are all significantly better than Atlanta, meaning it will be one-and-done for the Hawks. Elhassan: They'll put up the good fight before succumbing in six games in the first round. Grizzard: The Hawks are tough to predict with a new coach, new system and major roster turnover. If Horford has another All-Star season, the Hawks could be the sixth seed and a tough out in the first round. Williams' uncertain health makes it more likely the Hawks will win 43 games and be fodder for second-seeded Chicago in the first round. Han: Atlanta will strategically bottom out, missing the playoffs for the first time since drafting Horford. And with a combination of the pieces they retain along with the assets they acquire, the Hawks will be ready to roll the following season. Robb: A first-round exit in the postseason. The Hawks will be one of the better teams in the middle tier of the Eastern Conference, but they will fall short against the elite squads of the East. With a young talented core and cap flexibility in place, Atlanta's long-term future is brighter than ever though.",
                          "The Nets loaded up on marquee names this offseason. Will it translate to an on-court turnaround? What's in store for the Brooklyn Nets? Our panel of five looks back at the offseason moves (and nonmoves) and forward to what lies ahead in the 2013-14 NBA season. 1. What grade would you give the Nets' offseason? Jared Dubin, Hardwood Paroxysm: A-. Swapping out Gerald Wallace, MarShon Brooks and crew for Paul Pierce, Kevin Garnett, Jason Terry and Andrei Kirilenko is an on-court coup. The only reason I can't go higher is because Pierce and KG likely aren't long for Brooklyn and the Nets gave up so many future picks. Jeremy Gordon, Brooklyn's Finest: A cautious A. Sure, it could go to pieces the moment KG or Pierce takes extended time off because of age-related injury, but if the core holds and the team jells, the new additions should take the Nets from first-round exit to fringe contender. Israel Gutierrez, ESPN.com: B. Forget about the cost of this experienced group, because the owner did. All you can ask is to be in the conversation. The Nets are, if only for this season, and maybe it would take another trade or an unfortunate injury elsewhere. Either way, the Nets' offseason made them better and more intriguing. Mike Mazzeo, ESPN New York: A-. The future first-round picks they surrendered to acquire KG and Pierce could come back to haunt the franchise long term, but with a win-now mandate, GM Billy King did a lot more with his cap-strapped roster than I thought he could. The AK-47 and Andray Blatche signings were tremendous value, the Jason Kidd hire a worthwhile gamble. Ohm Youngmisuk, ESPN New York: A. The hiring of Kidd gave the franchise an identity and a leader. The Nets then catapulted into contender status by landing Pierce, Garnett and Terry, with Kidd playing a pivotal role in helping convince Pierce and Garnett to come. And don't overlook the addition of Kirilenko. 2. What's the biggest question facing the Nets in 2013-14? Dubin: Health. Simply put, Pierce and, to a lesser extent, Garnett wore down by the end of last season. How Kidd manages the minutes of his older players to keep them healthy could be the difference between another first-round loss and a trip to the Eastern Conference finals. Gordon: Health. The chemistry shouldn't be a problem: There's a nice mix of veterans who get it, young stars hitting their window and rookies, or near-rookies, with potential. But, as mentioned before, keeping everyone on the floor is important to their playoff run. Gutierrez: There'll be quite a few, but probably of greatest interest is how the scoring will be distributed, which puts a lot of pressure on Deron Williams to orchestrate it all. Mazzeo: A first-year coach and chemistry are big questions, but I think health is the biggest. KG (no back-to-backs?) and Pierce aren't young. D-Will's ankles are a concern. Brook Lopez is coming off his third foot surgery in 18 months. Joe Johnson was bothered by plantar fasciitis in the playoffs. Yeah, staying healthy for the postseason is key. Youngmisuk: Can Kidd lead the Nets back to the NBA Finals as a coach? Owner Mikhail Prokhorov is taking a risk in handing over a roster built to win it all to a rookie coach. How will Kidd manage egos, substitution patterns and last-second situations? The bet here is that Kidd's Hall of Fame vision, high basketball IQ and on-court leadership skills will transfer to the bench and complement the mixture of All-Star veterans. 3. Who's the Nets' most intriguing player? Dubin: Kililenko. AK-47 will likely operate as the swing man between small and big lineups. Who and how many of Johnson, Pierce, Garnett and Lopez he shares the floor with will shape how the Nets play at any given time he's on the court. Gordon: Garnett. He's responsible for upgrading Brooklyn's defense and keeping everyone in line with his de facto aggro tone, but he'll have a harder time doing that if he's wearing street clothes. Fortunately, it doesn't seem like he's ready to hit the bench any time soon. Gutierrez: Based on my previous answer, it has to be Williams. We know what Pierce, Johnson and Garnett can do, but will Williams let them get it done, all while making sure Lopez continues to improve? Oh, and he'll probably need to listen closest to Kidd. Mazzeo: KG is here to change the culture and improve the defense, but it's gotta be AK-47. Dude can play/guard so many different positions, and that versatility is so useful. He can also start if they want to rest KG. A $3.2 million salary was a flat-out bargain. Youngmisuk: Kirilenko could play a pivotal role if he can stay healthy. He is capable of defending multiple positions, can score some, rebound and block shots. Kirilenko doesn't have to play huge minutes, but he can start in case of injury or a vet like Garnett needs to be rested. He might even be an option to finish some games for Kidd because of his length and defensive ability. 4. What's one bold prediction about the Nets? Dubin: Kidd's lack of prior NBA head-coaching experience won't make much of a difference in Brooklyn's regular-season win-loss record. The Nets have enough talent to win regular-season games regardless of Kidd's tactical maneuvers. The playoffs -- in which teams have to prepare for only one opponent -- are where his mettle will be tested. Gordon: They'll compete for the No. 1 seed as the team gets it together almost immediately, with Deron and Brook playing like unquestioned All-Stars and KG, Pierce, AK-47, Johnson and the rest of the bench providing stellar support. They might not finish ahead of Miami or Chicago, but it'll be close. Gutierrez: Not sure how bold this is, but Terry will look much more like the Mavs version of himself than the Celtics version. Kidd knows how Terry rolls, and the coach will certainly need Terry off his bench because just about all his offense will presumably be in the starting lineup. Mazzeo: With good friend Kidd coaching him, Williams will lead the NBA in assists and merit MVP consideration after an up-and-down, injury-plagued 2012-13 campaign. The new system and supporting cast will benefit him greatly. Youngmisuk: Under Kidd's direction, a rejuvenated Williams will return to elite point-guard status and average more than 10 assists for the first time since 2010-11. Williams has struggled with injuries and, admittedly, his confidence in the past few seasons but will return to being a dominant point guard with his most talented supporting cast around him. 5. Prediction time: How far will the Nets go this season? Dubin: Eastern Conference semifinals. Indiana and Chicago, by virtue of younger, healthier rosters, have a better shot of making it out of the East than Brooklyn does. Gordon: Seeding is important, but if they finish in the top three, the Eastern Conference finals don't seem like an unfair bet -- to say nothing of the Finals if they manage to get a lucky break (like Dwyane Wade or Joakim Noah tuckering out by that point of the season). Gutierrez: Deep … into the second round of the playoffs. I'm waiting for the bold person to say the Nets are a conference finalist ahead of the Heat, Pacers and Bulls. I am not that person. Mazzeo: Eastern Conference finals. But with this team, who knows? Anything less than an epic second-round series with an elite team like Miami or Indiana would be a colossal failure. The pressure is on. Their window is now, but this is a superstar's league, and there are no LeBrons or Durants on this squad. Youngmisuk: The Nets will win 50 games because it will take them some time to develop chemistry while Kidd also tries to keep his older veterans fresh for the postseason. The Nets will then advance to the Eastern Conference finals and perhaps give defending champion Miami a run for its money." };
  /*    for (String str : example) {
        System.out.println(classifier.classifyToString(str));
      }
      System.out.println("---");

      for (String str : example) {
        // This one puts in spaces and newlines between tokens, so just print not println.
        System.out.print(classifier.classifyToString(str, "slashTags", false));
      }
      System.out.println("---");
*/
      for (String str : example) {
        String text = classifier.classifyWithInlineXML(str);
        System.out.println(text);
        Set<String> r = getEntities(text);
        for (String term: r) {
          System.out.println(term);
        }
      }
      System.out.println("---");
/*
      for (String str : example) {
        System.out.println(classifier.classifyToString(str, "xml", true));
      }
      System.out.println("---");*/
/*
      int i=0;
      for (String str : example) {
        for (List<CoreLabel> lcl : classifier.classify(str)) {
          for (CoreLabel cl : lcl) {
            System.out.print(i++ + ": ");
            System.out.println(cl.toShorterString());
          }
        }
      }*/
    }
  }

  //get all the name entities in the text
  public static Set<String> getEntities(String text) {
    Set<String> entityset = new HashSet<String>();
    
    Pattern p = Pattern.compile("<[A-Z]+>.+?</[A-Z]+>");
    Pattern rmTag = Pattern.compile("</?[A-Z]+>");
    Matcher m = p.matcher(text);
    
    while (m.find()) {
      System.out.println(m.group());
      Matcher tm = rmTag.matcher(m.group()); //remove tags
      String temp = tm.replaceAll("");
      
      if (!entityset.contains(temp.toLowerCase())) {
        entityset.add(temp.toLowerCase());
      }
    }
    
    return entityset;
  }
}
