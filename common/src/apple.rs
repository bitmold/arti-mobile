use crate::{start_arti_proxy, stop_arti_proxy};

use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int};
use std::ptr::null;

type LoggingCallback = extern "C" fn(*const c_char);

#[no_mangle]
pub extern "C" fn start_arti(
    state_dir: *const c_char,
    cache_dir: *const c_char,
    obfs4_port: c_int,
    snowflake_port: c_int,
    obfs4proxy_path: *const c_char,
    bridge_line: *const c_char,
    socks_port: c_int,
    dns_port: c_int,
    log_fn: LoggingCallback,
) -> *mut c_char {
    let state_dir = unsafe { CStr::from_ptr(state_dir) }.to_string_lossy();
    let cache_dir = unsafe { CStr::from_ptr(cache_dir) }.to_string_lossy();

    let obfs4proxy_path = if obfs4proxy_path == null() {
        None
    } else {
        unsafe { CStr::from_ptr(obfs4proxy_path) }.to_str().ok()
    };

    let bridge_line = if bridge_line == null() {
        None
    } else {
        unsafe { CStr::from_ptr(bridge_line) }.to_str().ok()
    };

    let result = match start_arti_proxy(
        &cache_dir,
        &state_dir,
        obfs4_port as u16,
        snowflake_port as u16,
        obfs4proxy_path,
        bridge_line,
        socks_port as u16,
        dns_port as u16,
        move |buf: &[u8]| {
            let cstr = CString::new(buf.to_owned()).unwrap();
            (log_fn)(cstr.as_ptr());
        },
    ) {
        Ok(res) => format!("Output: {}", res),
        Err(e) => format!("Error: {}", e),
    };

    CString::new(result).unwrap().into_raw()
}

#[no_mangle]
pub extern "C" fn stop_arti() {
    stop_arti_proxy();
}
