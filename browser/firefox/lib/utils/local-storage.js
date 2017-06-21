/**
 * This file is part of Adguard Browser Extension (https://github.com/AdguardTeam/AdguardBrowserExtension).
 *
 * Adguard Browser Extension is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Adguard Browser Extension is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Adguard Browser Extension.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Local storage implementation for firefox
 */
adguard.localStorageImpl = (function (adguard) {

    var getItem = function (key) {
        return adguard.SimplePrefs.get(key);
    };

    var setItem = function (key, value) {
        adguard.SimplePrefs.set(key, value);
    };

    var removeItem = function (key) {
        adguard.SimplePrefs.remove(key);
    };

    var hasItem = function (key) {
        return adguard.SimplePrefs.has(key);
    };

    return {
        getItem: getItem,
        setItem: setItem,
        removeItem: removeItem,
        hasItem: hasItem
    };

})(adguard);