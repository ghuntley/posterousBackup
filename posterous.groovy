
/**
Paternité-Pas d'Utilisation Commerciale-Partage des Conditions Initiales à l'Identique 2.0 France
Vous êtes libres :
de reproduire, distribuer et communiquer cette création au public
de modifier cette création
Selon les conditions suivantes :

Paternité — Vous devez citer le nom de l'auteur original de la manière indiquée par l'auteur de l'oeuvre ou le titulaire des droits qui vous confère cette autorisation (mais pas d'une manière qui suggérerait qu'ils vous soutiennent ou approuvent votre utilisation de l'oeuvre). 



Pas d'Utilisation Commerciale — Vous n'avez pas le droit d'utiliser cette création à des fins commerciales. 

Partage des Conditions Initiales à l'Identique — Si vous modifiez, transformez ou adaptez cette création, vous n'avez le droit de distribuer la création qui en résulte que sous un contrat identique à celui-ci. 
With the understanding that:
Waiver — Any of the above conditions can be waived if you get permission from the copyright holder. 
Other Rights — In no way are any of the following rights affected by the license: 
Your fair dealing or fair use rights; 
The author's moral rights; 
Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights. 
Notice — A chaque réutilisation ou distribution de cette création, vous devez faire apparaître clairement au public les conditions contractuelles de sa mise à disposition. La meilleure manière de les indiquer est un lien vers cette page web. 
*/
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import groovy.xml.DOMBuilder;

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import groovy.lang.Grab;
import groovy.util.CliBuilder;
import groovy.util.XmlSlurper;
import groovyx.net.http.HTTPBuilder;
import groovyx.net.http.URIBuilder;

import groovy.xml.MarkupBuilder;
import groovy.xml.StreamingMarkupBuilder;
import groovy.xml.XmlUtil;
import groovy.xml.dom.DOMUtil;

/**
 * A psoterous export application. It takes a posterous site as input and outputs a bunch of "xml" files with linked content
 * @author Nicolas Delsaux (nicolas.delsaux@gmail.com)
 */
class Posterous {
	private static final String INPUT_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z"
	private static final DateFormat INPUT_FORMATTER = new SimpleDateFormat(INPUT_DATE_FORMAT, Locale.US);
	private static final String OUTPUT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
	private static final DateFormat OUTPUT_FORMATTER = new SimpleDateFormat(OUTPUT_DATE_FORMAT);
	
	private String username;
	private String password;
	private String outputFolder;
	private String xsl = null;
	
	/**
	 * Main loop
	 */
	public void run(separeMedia, downloadThumbnails) {
		HTTPBuilder builder = new HTTPBuilder("http://posterous.com");
		builder.auth.basic "posterous.com", 80, username, password
		Collection sites = getSites(builder);
		int siteIndex = 1;
		sites.eachWithIndex() {site, i ->
			Collection posts = site.getPosts(downloadThumbnails);
			posts.eachWithIndex() {post, j->
				println "saving post ${j+1}/$site.postCount of site ${i+1} linked to ${post.link}"
				post.prepare(outputFolder, site.postsUrls, separeMedia)
				post.save(outputFolder)
			}
		}
	}
	
	/**
	 * @param HTTPBuilder used to do the requests
	 * @return a collection of expando on which the getposts method can be called
	 */
	private Collection getSites(HTTPBuilder builder) {
		Collection returned = [];
		builder.get(path: "/api/getsites") { resp, xml ->
			// reply should be OK, elsewhere an exception has already been fired
			for(site in xml.site) {
				Expando siteInfos = new Expando();
				siteInfos.id=site.id.text();
				siteInfos.name=site.name.text();
				siteInfos.url=site.url.text();
				siteInfos.commentsEnabled=site.commentsenabled.text();
				siteInfos.postCount=site.num_posts.text() as Integer;
				siteInfos.getPosts = createGetPosts(builder)
				siteInfos.postsUrls = [:];
				returned << siteInfos;
			}
		}
		return returned;
	}
	
	/**
	 * Create a getPosts closure for an expando
	 */
	def createGetPosts(HTTPBuilder builder) {
		return {downloadThumbnails ->
			int currentPost = 0;
			int postsPerPage = 50;
			int pagesCount = postCount/postsPerPage+1 as Integer
			returned = []
			while(currentPost<postCount) {
				def pageIndex = (currentPost/postsPerPage)+1;
				try {
					// Solution sent by Tom Nichols : http://permalink.gmane.org/gmane.comp.lang.groovy.user/47128
					builder.headers[ 'Authorization' ] ="$username:$password".getBytes('iso-8859-1').encodeBase64()
					builder.get(path:"/api/readposts",
							query : [site_id: id, num_posts: postsPerPage, page:(pageIndex)]) { resp, xml ->
								//								println "now processing page "+pageIndex+" of "+(pagesCount)
								for(post in xml.post) {
									Expando postObject = new Expando();
									postObject.title = post.title.text()
									postObject.link = post.link.text()
									postObject.linkURI = new URI(postObject.link);
									postsUrls[postObject.link] = postObject;
									URI uri = new URI(postObject.link)
									postObject.body = post.body.text()
									postObject.date = parseDate(post.date.text())
									postObject.view = post.views.text()
									postObject.private = post.private.text()
									postObject.author = post.author.text()
									postObject.authorpic = post.authorpic.text()
									postObject.comments = []
									for(comment in post.comment) {
										def c = [:]
										c.author = comment.author.text()
										c.authorpic = comment.authorpic.text()
										c.body = comment.body.text()
										c.date = parseDate(comment.date.text())
										postObject.comments << c
									}
									postObject.tags = []
									for(t in post.tag) {
										postObject.tags << t.text();
									}
									// Now, we only need associated media download : http://docs.codehaus.org/display/GROOVY/Simple+file+download+from+URL
									postObject.medias = [:]
									int index = 0;
									for(media in post.media) {
										def type = media.type.text()
										switch(type) {
											case "image":
												postObject.medias.put(media.medium.url.text(), uri.getPath()+"_"+(index++))
												if(downloadThumbnails)
													postObject.medias.put(media.thumb.url.text(), uri.getPath()+"_"+(index++))
												break;
											case "audio":
												postObject.medias.put(media.url.text(), uri.getPath()+"_"+(index++))
												break;
											case "video":
												postObject.medias.put(media.url.text(), uri.getPath()+"_"+(index++))
												postObject.medias.put(media.thumb.text(), uri.getPath()+"_"+(index++))
												postObject.medias.put(media.flv.text(), uri.getPath()+"_"+(index++))
												postObject.medias.put(media.mp4.text(), uri.getPath()+"_"+(index++))
												break;
										}
										index++;
									}
									
									postObject.prepare = createPrepare()
									postObject.save = createSave()

									returned << postObject
								}
								currentPost += postsPerPage;
							}
				} catch(Exception e) {
					URIBuilder error = new URIBuilder(builder.uri.toString());
					error.path = "/api/readposts"
					error.addQueryParam("site_id", id);
					error.addQueryParam("num_posts", postsPerPage);
					error.addQueryParam("page", pageIndex);
					println "something went wrong parsing "+error.toString()+"\n"+e.getMessage()+"\n"
					e.printStackTrace()
				}
				
			}
			return returned;
		};
	}
	
	def parseDate(String text) {
		Date d = INPUT_FORMATTER.parse(text);
		return OUTPUT_FORMATTER.format(d);
	}
	
	/**
	 * Create the prepare closure. This closure will handle download of files and post content rewriting to use relative urls to images
	 */
	@Grab(group='nekohtml', module='nekohtml', version='0.9.5')
	def createPrepare() {
		return { folder, postsUrls, separeMedia -> 
			URI uri = linkURI
			String directory = folder+"/"+uri.getHost()
			// Replace all found urls
			postsUrls.each { url, postObject ->
				if(body.contains(url)) {
					def path = postObject.linkURI.getPath();
					path = path.substring(1, path.size())+".jsg.xml";
					body = body.replaceAll(url, "{relocatable: "+path+"}");
				}
			}
			if(medias.size()>0) {
				def usable=[:]
				medias.each { mediaUrl, expectedFile ->
					// get media url ext
					def ext="";
					if(mediaUrl.contains(".")) {
						ext = mediaUrl[mediaUrl.lastIndexOf(".")..mediaUrl.size()-1]
//					println "should assemble directory $directory, expectedFile $expectedFile and ext $ext"
						// This is a workarround for path containing trailing /
						def destinationName = expectedFile[1..expectedFile.size()-1]+ext
						usable[mediaUrl] = destinationName
						File expectedOutput = null
						if(separeMedia) {
							fullDestination = uri.getPath()[1..uri.getPath().size()-1]+"/"+destinationName;
							expectedOutput = new File(directory+"/"+fullDestination)
							// Think about url re-writing
							body = body.replaceAll(mediaUrl, fullDestination);
						} else {
							expectedOutput = new File(directory+"/"+destinationName)
						}
						expectedOutput.parentFile.mkdirs()
						expectedOutput.setWritable(true);
						expectedOutput.parentFile.mkdirs();
						if(expectedOutput.exists()) {
							expectedOutput.delete()
						}
						def file = new FileOutputStream(expectedOutput)
						def out = new BufferedOutputStream(file)
						out << new URL(mediaUrl).openStream()
						out.close()
					} else {
						println "in message "+uri.toString()+", there is one media which couldn't be downloaded : \""+mediaUrl+"\""
					}
				}
				// Now replace all references to downloaded elements
				usable.each { mediaUrl, expectedFile ->
					body  = body.replaceAll(mediaUrl, expectedFile);
				}
			}
		}
	}
	
	def createSave() {
		return { folder ->
			URI uri = new URI(link)
			File toWrite = new File(folder+"/"+uri.getHost()+"/"+uri.getPath()+".jsg.xml")
			def writer = new StringWriter()
			def xml = new MarkupBuilder(writer)
			xml.pi("xml":["version":"1.0"])
			if(xsl!=null && xsl.trim().length()>0) {
				xml.pi("xml-stylesheet":["href":xsl, "type":"text/xsl"])
			}
			xml.post() {
				title(title)
				date(date)
				if(tags.size()>0) {
					tags {
						for(t in tags) {
							tag(t);
						}
					}
				}
				author {
					name(author)
					pic(authorpic)
				}
				body('') {
					yieldUnescaped("<![CDATA["+body+"]]>")
				}
				if(comments.size()>0) {
					comments {
						for(c in comments) {
							comment {
								author {
									name(c.author)
									pic(c.authorpic)
								}
								date(c.date)
								body('') {
									yieldUnescaped("<![CDATA["+c.body+"]]>")
								}
							}
						}
					}
				}
			}
			toWrite.parentFile.mkdirs();
			if(toWrite.exists()) {
				toWrite.setWritable(true);
				toWrite.delete()
			}
			toWrite.write(writer.toString(), 'UTF-8')
		};
	}
	
	/**
	 * Main method, doing the grab stuff and providing usual infos
	 */
	@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.0-RC2')
	static void main(args) throws Exception {
		println 'This is posterous export script v 0.6'
		println 'You like that script ? You already use flattr ? Please go to http://flattr.com/thing/54243/Posterous-backup-script to show your appreciation'
		def cli = new CliBuilder(usage:'groovy posterous.groovy -u email@posterous -p password -o outputFolder')
		cli.h(longOpt: 'help', 'provides full help and usage information')
		cli.u(longOpt: 'username', 'Sets posterous mail address here', args:1, required:true)
		cli.p(longOpt: 'password', 'Unfortunatly one have to give its password to this little script', args:1, required:true)
		cli.o(longOpt: 'output', 'An eventually existing output folder, where all data will be output. Beware, if some data exists in that folder, it may be overwritten.', args:1, required:true)
		cli.s(longOpt: 'separeMedia', 'Separates media from posts directory. When set, all medias are copied in subfolders of output folders named as posts they\'re associated with. This option is by request of Eli Weinberg, with my best wishes.', args:0, required:false)
		cli.d(longOpt: 'downloadThumbnails', 'When present, thumbnails as well as normal size images are downloaded. This option is by request of Eli Weinberg, with my best wishes.', args:0, required:false)
		cli.x(longOpt: 'xsl', 'Gives an XSL stylmesheet URL that will be put in all generated files', args:1, required:false)
		def opt = cli.parse(args);
		if(!opt) {
			cli.usage
		} else {
			Posterous app = new Posterous();
			if(opt.h)
				cli.usage();
			app.username = opt.u;
			app.password = opt.p;
			app.outputFolder = opt.o
			app.xsl = opt.x
			app.run(opt.s, opt.d);
		}
	}
}
