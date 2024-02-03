import os, re, argparse

def build(label: str, package: str, url: str):
    '''Build APK with updated strings to package name/ApplicationID'''
    os.system(f'.\\gradlew assembleDebug -PpkgName="{package}" -PappName="{label}" -PappURL="{url}" -PAPKName="{package}.apk"')

def validate_package(package: str):
    if not re.match(r'^[a-z0-9.]+$', package):
        raise argparse.ArgumentTypeError(f"Invalid package name: {package}")
    return package

def validate_url(url: str):
    if not re.match(r'^(https?)?://(\S+)$', url):
        raise argparse.ArgumentTypeError(f"Invalid URL: {url}")
    return url

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Build an APK with specified parameters.")
    parser.add_argument('-n', '--name', type=str, help='App name')
    parser.add_argument('-p', '--package', type=validate_package, help='Package name, e.g., com.webapp.example')
    parser.add_argument('-u', '--url', type=validate_url, help='URL, e.g., https://proton.me')
    args = parser.parse_args()

    lbl = args.name
    pkg = args.package
    url = args.url

    if not lbl:
        lbl = input('App name ("Example app"): ')
    while not pkg:
        pkg_input = input('Package name ("com.webapp.example"): ')
        try:
            pkg = validate_package(pkg_input)
        except argparse.ArgumentTypeError as e:
            print(e)
    while not url:
        url_input = input('URL ("https://proton.me"): ')
        try:
            url = validate_url('https://' + url_input if not url_input.startswith(('http://', 'https://')) else url_input)
        except argparse.ArgumentTypeError as e:
            print(e)

    # Main script execution
    build(lbl, pkg, url)
    print("APK build complete.")
