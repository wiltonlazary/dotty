package dotty.tools.scaladoc

import dotty.tools.dotc.config.Settings._
import dotty.tools.dotc.config.AllScalaSettings

class ScaladocSettings extends SettingGroup with AllScalaSettings:
  val unsupportedSettings = Seq(
    // Needed for plugin architecture
    plugin, disable, require, pluginsDir, pluginOptions,
  )


  val projectName: Setting[String] =
    StringSetting("-project", "project title", "The name of the project.", "", aliases = List("-doc-title"))

  val projectVersion: Setting[String] =
    StringSetting("-project-version", "project version", "The current version of your project.", "", aliases = List("-doc-version"))

  val projectLogo: Setting[String] =
    StringSetting("-project-logo", "project logo filename", "Path to the file that contains the project's logo. Provided path can be absolute or relative to the project root directory.", "", aliases = List("-doc-logo"))

  val projectFooter: Setting[String] = StringSetting("-project-footer", "project footer", "A footer on every Scaladoc page.", "", aliases = List("-doc-footer"))

  val sourceLinks: Setting[List[String]] =
    MultiStringSetting("-source-links", "sources", SourceLinks.usage)

  val legacySourceLink: Setting[String] =
    StringSetting("-doc-source-url", "sources", "Legacy option from Scala 2. Use -source-links instead.", "")

  val syntax: Setting[List[String]] =
    MultiStringSetting("-comment-syntax", "syntax", tasty.comments.CommentSyntaxArgs.usage)

  val revision: Setting[String] =
    StringSetting("-revision", "revision", "Revision (branch or ref) used to build project project", "")

  val externalDocumentationMappings: Setting[List[String]] =
    MultiStringSetting("-external-mappings", "external-mappings",
      "Mapping between regexes matching classpath entries and external documentation. " +
        "'regex::[scaladoc|scaladoc|javadoc]::path' syntax is used")

  val legacyExternalDocumentationMappings: Setting[List[String]] =
    MultiStringSetting("-doc-external-doc", "legacy-external-mappings", "Legacy option from Scala 2. Mapping betweeen path and external documentation. Use -external-mappings instead.")

  val socialLinks: Setting[List[String]] =
    MultiStringSetting("-social-links", "social-links",
      "Links to social sites. '[github|twitter|gitter|discord]::link' or 'custom::link::light_icon_file_name[::dark_icon_file_name]' syntax is used. For custom links, the icons must be present in '_assets/images/'")

  val deprecatedSkipPackages: Setting[List[String]] =
    MultiStringSetting("-skip-packages", "packages", "Deprecated, please use `-skip-by-id` or `-skip-by-regex`")

  val skipById: Setting[List[String]] =
    MultiStringSetting("-skip-by-id", "package or class identifier", "Identifiers of packages or top-level classes to skip when generating documentation")

  val skipByRegex: Setting[List[String]] =
    MultiStringSetting("-skip-by-regex", "regex", "Regexes that match fully qualified names of packages or top-level classes to skip when generating documentation")

  val docRootContent: Setting[String] =
    StringSetting("-doc-root-content", "path", "The file from which the root package documentation should be imported.", "")

  val author: Setting[Boolean] =
    BooleanSetting("-author", "Include authors.", false)

  val groups: Setting[Boolean] =
    BooleanSetting("-groups", "Group similar functions together (based on the @group annotation)", false)

  val visibilityPrivate: Setting[Boolean] =
    BooleanSetting("-private", "Show all types and members. Unless specified, show only public and protected types and members.", false)

  val docCanonicalBaseUrl: Setting[String] =
    StringSetting(
      "-doc-canonical-base-url",
      "url",
      s"A base URL to use as prefix and add `canonical` URLs to all pages. The canonical URL may be used by search engines to choose the URL that you want people to see in search results. If unset no canonical URLs are generated.",
      ""
    )

  val siteRoot: Setting[String] = StringSetting(
    "-siteroot",
    "site root",
    "A directory containing static files from which to generate documentation.",
    "./docs"
  )

  val noLinkWarnings: Setting[Boolean] = BooleanSetting(
    "-no-link-warnings",
    "Avoid warnings for ambiguous and incorrect links in members look up. Doesn't affect warnings for incorrect links of assets etc.",
    false
  )

  val noLinkAssetWarnings: Setting[Boolean] = BooleanSetting(
    "-no-link-asset-warnings",
    "Avoid warnings for incorrect links of assets like images, static pages, etc.",
    false
  )

  val versionsDictionaryUrl: Setting[String] = StringSetting(
    "-versions-dictionary-url",
    "versions dictionary url",
    "A URL pointing to a JSON document containing a dictionary `version label -> documentation location`. " +
      "The JSON file has single property \"versions\" that holds dictionary of labels of specific docs and URL pointing to their index.html top-level file. " +
      "Useful for libraries that maintain different versions of their documentation.",
    ""
  )

  val YdocumentSyntheticTypes: Setting[Boolean] =
    BooleanSetting("-Ydocument-synthetic-types", "Attach pages with documentation of the intrinsic types e. g. Any, Nothing to the docs. Setting is useful only for stdlib.", false)

  val snippetCompiler: Setting[List[String]] =
    MultiStringSetting("-snippet-compiler", "snippet-compiler", snippets.SnippetCompilerArgs.usage)

  val generateInkuire: Setting[Boolean] =
    BooleanSetting("-Ygenerate-inkuire", "Generates InkuireDB and enables Hoogle-like searches", false)

  val apiSubdirectory: Setting[Boolean] =
    BooleanSetting("-Yapi-subdirectory", "Put the API documentation pages inside a directory `api/`", false)

  val scastieConfiguration: Setting[String] =
    StringSetting("-scastie-configuration", "Scastie configuration", "Additional configuration passed to Scastie in code snippets", "")

  val defaultTemplate: Setting[String] =
    StringSetting(
      "-default-template",
      "default template used by static site",
      "The static site is generating empty files for indexes that haven't been provided explicitly in a sidebar/missing index.html in directory. " +
        "User can specify what default template should be used for such indexes. It can be useful for providing generic templates that interpolate some common settings, like title, or can have some custom html embedded.",
      ""
    )

  val quickLinks: Setting[List[String]] =
    MultiStringSetting(
      "-quick-links",
      "quick-links",
      "List of quick links that is displayed in the header of documentation."
    )

  val dynamicSideMenu: Setting[Boolean] =
    BooleanSetting("-dynamic-side-menu", "Generate side menu via JS instead of embedding it in every html file", false)

  def scaladocSpecificSettings: Set[Setting[?]] =
    Set(sourceLinks, legacySourceLink, syntax, revision, externalDocumentationMappings, socialLinks, skipById, skipByRegex, deprecatedSkipPackages, docRootContent, snippetCompiler, generateInkuire, defaultTemplate, scastieConfiguration, quickLinks, dynamicSideMenu)
