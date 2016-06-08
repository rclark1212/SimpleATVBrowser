# SimpleATVBrowser

(Not so) Simple example of a browser for ATV
(used to be simple)

# Summary

Uses webview and a webview client to deliver a simple browser. All browser functionality is leveraged from the existing webview. Cookies, javascript have been enabled. Basic history is also supported. As is voice input. Note that plugins are *not* supported so some web sites will not work.

# Use

Build and select info button for controller mapping.

# Functionality

There are two primary views - a webview and a favorites view. You flip between these views using the home button at top. If there are no favorites, home will take you to google (and launch takes you to google). If there are favorites, home/launch take you to the favorites page.

# Web Page

Standard web page with info/help, home, favorite (heart), back, forward mic, refresh buttons along with an address bar with a drop down for last web sites visited.

Favorite is a toggle for a web address. When heart is solid, web page is a favorite. When empty, not a favorite.

Home has dual roles - if no favorites, takes you to google. If favorites, takes you to the favorites page.

Mic button allows voice input for a web address. Note that the mic button on the shield controller does same.

Left trigger/right trigger zoom in this view. Left stick scrolls web page. Right stick acts as a mouse.

# Favorites Page

Shows a browse view of pages marked as favorite. Click a web page card and you will be taken to the web page view of that website.

# Address Processing

Is extremely simple logic. We look for 2 "." in the string. If we don't find http or https in the string, we prepend http. We then check if this is a valid URL. If so, we load it. If not, we kick off a google search with the original string.

Once loading has *completed*, we then check the URL (as you could have been redirected) and we update the edit box address. We also check for favorites here (favorites uses the native URL of the web page).

# Other

Originally intented to drop this onto the google play store as a free browser app. Discovered, however, that google play policies prohibited web browsers for android tv. Ugg. So no play store for SimpleATVBrowser. Leaving it on github though for folks to build themselves if they so choose (or, if you want a signed prebuilt, leaving one in the /app directory).

Note that I do not expect to be active on this project - it does what it needs for me and without ability to distribute, there won't be any user base to speak of to do bug fixes for. i.e. you should clone and modify rather than rely on pull.
