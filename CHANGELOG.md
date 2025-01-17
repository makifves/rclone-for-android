<!--
    When adding new changelog entries, use [Issue #0] to link to issues and
    [PR #0] to link to pull requests. Then run:

        ./gradlew changelogUpdateLinks

    to update the actual links at the bottom of the file.
-->

### Version 1.17

* Update dependencies ([PR #38])
* Disable arm64 memory tagging extensions support ([PR #39])
  * Golang's cgo runtime currently does not support MTE and would cause rclone/RSAF to crash.
  * This workaround only affects people who explicitly enable MTE for user apps on the Pixel 8 series or future ARMv9 devices.

### Version 1.16

* Update rclone to 1.64.2 ([PR #37])

### Version 1.15

* Update rclone to 1.64.1 ([PR #36])

### Version 1.14

* Update dependencies and target API 34 ([PR #34])

### Version 1.13

* Update rclone to 1.64.0 ([PR #33])

### Version 1.12

* All external app access to files on hidden remotes is now blocked ([Issue #27], [PR #31])
* Add option to lock app settings behind biometric/PIN/password unlock ([Issue #27], [PR #32])

### Version 1.11

* Add option to request "all files" permission on Android 11+ to allow wrapper remotes, like `crypt`, to access `/sdcard` ([Issue #28], [PR #29])
* Update all dependencies ([PR #30])

### Version 1.10

* Update rclone to 1.63.1 ([PR #26])
* Enable checksum validation for all gradle dependencies ([PR #25])

### Version 1.9

* Fix crash when cancelling the Edit Remote dialog on Android <=12 ([Issue #16], [PR #24])

### Version 1.8

* Fix race condition in rclone initialization that could lead to crashes when accessing files while RSAF's user interface is closed ([Issue #22], [PR #23])

### Version 1.7

* Update rclone to 1.63.0 ([PR #20])
* Reduce directory cache time from 5 minutes to 5 seconds ([PR #21])
  * After a file copy or move, the files in the target directory will no longer appear as missing for 5 minutes. Due to API limitations, there's no way to manually invalidate the cache after a copy/move operation, so a short timeout is used instead.

### Version 1.6

* Fix hang when hiding remotes that use OAuth2 authentication ([Issue #16], [PR #19])

### Version 1.5

* Add support for hiding remotes from DocumentsUI ([Issue #16], [PR #17])
* Update dependencies ([PR #18])

### Version 1.4

* Improve UX for resetting to current/default values and add support for revealing passwords in the interactive configuration dialog ([Issue #8], [PR #14])
* Add option to show all dialogs at the bottom of the screen ([Issue #9], [PR #15])

### Version 1.3

* Fix cache directory being set to non-writable `/data/local/tmp/` in certain contexts ([Issue #11], [PR #12])
* Work around upstream bug where passwords are not obscured in the config file, breaking password-based authentication (eg. smb, sftp) ([Issue #7], [PR #13])

### Version 1.2

* Update all dependencies ([PR #2], [PR #5])
* Fix `isChildDocument` returning false for nested children, which caused some apps to crash ([PR #3])

### Version 1.1

* Add option to open remotes in DocumentsUI ([PR #1])

### Version 1.0

* Initial release

<!-- Do not manually edit the lines below. Use `./gradlew changelogUpdateLinks` to regenerate. -->
[Issue #7]: https://github.com/chenxiaolong/RSAF/issues/7
[Issue #8]: https://github.com/chenxiaolong/RSAF/issues/8
[Issue #9]: https://github.com/chenxiaolong/RSAF/issues/9
[Issue #11]: https://github.com/chenxiaolong/RSAF/issues/11
[Issue #16]: https://github.com/chenxiaolong/RSAF/issues/16
[Issue #22]: https://github.com/chenxiaolong/RSAF/issues/22
[Issue #27]: https://github.com/chenxiaolong/RSAF/issues/27
[Issue #28]: https://github.com/chenxiaolong/RSAF/issues/28
[PR #1]: https://github.com/chenxiaolong/RSAF/pull/1
[PR #2]: https://github.com/chenxiaolong/RSAF/pull/2
[PR #3]: https://github.com/chenxiaolong/RSAF/pull/3
[PR #5]: https://github.com/chenxiaolong/RSAF/pull/5
[PR #12]: https://github.com/chenxiaolong/RSAF/pull/12
[PR #13]: https://github.com/chenxiaolong/RSAF/pull/13
[PR #14]: https://github.com/chenxiaolong/RSAF/pull/14
[PR #15]: https://github.com/chenxiaolong/RSAF/pull/15
[PR #17]: https://github.com/chenxiaolong/RSAF/pull/17
[PR #18]: https://github.com/chenxiaolong/RSAF/pull/18
[PR #19]: https://github.com/chenxiaolong/RSAF/pull/19
[PR #20]: https://github.com/chenxiaolong/RSAF/pull/20
[PR #21]: https://github.com/chenxiaolong/RSAF/pull/21
[PR #23]: https://github.com/chenxiaolong/RSAF/pull/23
[PR #24]: https://github.com/chenxiaolong/RSAF/pull/24
[PR #25]: https://github.com/chenxiaolong/RSAF/pull/25
[PR #26]: https://github.com/chenxiaolong/RSAF/pull/26
[PR #29]: https://github.com/chenxiaolong/RSAF/pull/29
[PR #30]: https://github.com/chenxiaolong/RSAF/pull/30
[PR #31]: https://github.com/chenxiaolong/RSAF/pull/31
[PR #32]: https://github.com/chenxiaolong/RSAF/pull/32
[PR #33]: https://github.com/chenxiaolong/RSAF/pull/33
[PR #34]: https://github.com/chenxiaolong/RSAF/pull/34
[PR #36]: https://github.com/chenxiaolong/RSAF/pull/36
[PR #37]: https://github.com/chenxiaolong/RSAF/pull/37
[PR #38]: https://github.com/chenxiaolong/RSAF/pull/38
[PR #39]: https://github.com/chenxiaolong/RSAF/pull/39
