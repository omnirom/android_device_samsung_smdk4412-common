/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by Marco Magliona <marco.magliona@gmail.com> See original files for differences
 *
 *
 */


//#define DEBUG_UEVENTS
#define CHARGER_KLOG_LEVEL 6

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/input.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/poll.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/un.h>
#include <time.h>
#include <unistd.h>

#include <sys/socket.h>
#include <linux/netlink.h>

#include <cutils/android_reboot.h>
#include <cutils/klog.h>
#include <cutils/list.h>
#include <cutils/misc.h>
#include <cutils/uevent.h>

#ifdef CHARGER_ENABLE_SUSPEND
#include <suspend/autosuspend.h>
#endif

#include "minui/minui.h"

#ifndef max
#define max(a,b) ((a) > (b) ? (a) : (b))
#endif

#ifndef min
#define min(a,b) ((a) < (b) ? (a) : (b))
#endif

#define ARRAY_SIZE(x)           (sizeof(x)/sizeof(x[0]))

#define MSEC_PER_SEC            (1000LL)
#define NSEC_PER_MSEC           (1000000LL)

#define BATTERY_UNKNOWN_TIME    (1 * MSEC_PER_SEC)
#define POWER_ON_KEY_TIME       (1 * MSEC_PER_SEC)
#define UNPLUGGED_SHUTDOWN_TIME (10 * MSEC_PER_SEC)

#define BATTERY_FULL_THRESH     95

#define LAST_KMSG_PATH          "/proc/last_kmsg"
#define LAST_KMSG_MAX_SZ        (32 * 1024)

#if 1
#define LOGE(x...) do { KLOG_ERROR("charger", x); } while (0)
#define LOGI(x...) do { KLOG_INFO("charger", x); } while (0)
#define LOGV(x...) do { KLOG_DEBUG("charger", x); } while (0)
#else
#define LOG_NDEBUG 0
#define LOG_TAG "charger"
#include <cutils/log.h>
#endif

enum supplies {NONE,AC,USB,BATTERY};

struct supplies_list {
  char ac_online_path[50];
  char usb_online_path[50];
  char battery_capacity_path[50];
};

struct key_state {
  bool pending;
  bool down;
  int64_t timestamp;
};

struct frame {
  const char *name;
  int disp_time;
  int min_capacity;
  bool level_only;

  gr_surface surface;
};

struct animation {
  bool run;

  struct frame *frames;
  int cur_frame;
  int num_frames;

  int cur_cycle;
  int num_cycles;

  /* current capacity being animated */
  int capacity;
};

struct charger {
  int64_t next_screen_transition;
  int64_t next_key_check;
  int64_t next_supply_check;
  struct key_state keys[KEY_MAX + 1];
  int online;
  int charger_type;
  int capacity;
  struct animation *batt_anim;
  gr_surface surf_unknown;
};

static struct frame batt_anim_frames[] = {
  {
    .name = "charger/battery_0",
    .disp_time = 750,
    .min_capacity = 0,
  },
  {
    .name = "charger/battery_1",
    .disp_time = 750,
    .min_capacity = 20,
  },
  {
    .name = "charger/battery_2",
    .disp_time = 750,
    .min_capacity = 40,
  },
  {
    .name = "charger/battery_3",
    .disp_time = 750,
    .min_capacity = 60,
  },
  {
    .name = "charger/battery_4",
    .disp_time = 750,
    .min_capacity = 80,
    .level_only = true,
  },
  {
    .name = "charger/battery_5",
    .disp_time = 750,
    .min_capacity = BATTERY_FULL_THRESH,
  },
};

static struct animation battery_animation = {
  .frames = batt_anim_frames,
  .num_frames = ARRAY_SIZE(batt_anim_frames),
  .num_cycles = 3,
};

static struct supplies_list *supplies_list;
static struct supplies_list supply;
static struct animation battery_animation;
static struct charger charger_state;

static int char_width;
static int char_height;

/* current time in milliseconds */
static int64_t curr_time_ms(void)
{
  struct timespec tm;
  clock_gettime(CLOCK_MONOTONIC, &tm);
  return tm.tv_sec * MSEC_PER_SEC + (tm.tv_nsec / NSEC_PER_MSEC);
}

static void clear_screen(void)
{
  gr_color(0, 0, 0, 255);
  gr_fill(0, 0, gr_fb_width(), gr_fb_height());
};

#define MAX_KLOG_WRITE_BUF_SZ 256

static void dump_last_kmsg(void)
{
  char *buf;
  char *ptr;
  unsigned sz = 0;
  int len;

  LOGI("\n");
  LOGI("*************** LAST KMSG ***************\n");
  LOGI("\n");
  buf = load_file(LAST_KMSG_PATH, &sz);
  if (!buf || !sz) {
    LOGI("last_kmsg not found. Cold reset?\n");
    goto out;
  }

  len = min(sz, LAST_KMSG_MAX_SZ);
  ptr = buf + (sz - len);

  while (len > 0) {
    int cnt = min(len, MAX_KLOG_WRITE_BUF_SZ);
    char yoink;
    char *nl;

    nl = memrchr(ptr, '\n', cnt - 1);
    if (nl)
      cnt = nl - ptr + 1;

    yoink = ptr[cnt];
    ptr[cnt] = '\0';
    klog_write(6, "<6>%s", ptr);
    ptr[cnt] = yoink;

    len -= cnt;
    ptr += cnt;
  }

  free(buf);

 out:
  LOGI("\n");
  LOGI("************* END LAST KMSG *************\n");
  LOGI("\n");
}

static int read_file(const char *path, char *buf, size_t sz)
{
  int fd;
  size_t cnt;

  fd = open(path, O_RDONLY, 0);
  if (fd < 0)
    goto err;

  cnt = read(fd, buf, sz - 1);
  if (cnt <= 0)
    goto err;
  buf[cnt] = '\0';
  if (buf[cnt - 1] == '\n') {
    cnt--;
    buf[cnt] = '\0';
  }

  close(fd);
  return cnt;

 err:
  if (fd >= 0)
    close(fd);
  return -1;
}

static int read_file_int(const char *path, int *val)
{
  char buf[32];
  int ret;
  int tmp;
  char *end;

  ret = read_file(path, buf, sizeof(buf));
  if (ret < 0)
    return -1;

  tmp = strtol(buf, &end, 0);
  if (end == buf ||
      ((end < buf+sizeof(buf)) && (*end != '\n' && *end != '\0')))
    goto err;

  *val = tmp;
  return 0;

 err:
  return -1;
}

static int get_battery_capacity()
{
  int ret;
  int batt_cap = -1;

  ret = read_file_int(supplies_list->battery_capacity_path, &batt_cap);
  if (ret < 0 || batt_cap > 100) {
    batt_cap = -1;
  }

  return batt_cap;
}

static void init_sysfs_paths(){
  strcpy(supplies_list->ac_online_path,AC_SYSFS);
  strcat(supplies_list->ac_online_path,"/online");
  LOGI("ac_path %s\n",supplies_list->ac_online_path);
  strcpy(supplies_list->usb_online_path,USB_SYSFS);
  strcat(supplies_list->usb_online_path,"/online");
  LOGI("usb_path %s\n",supplies_list->usb_online_path);
  strcpy(supplies_list->battery_capacity_path,BATTERY_SYSFS);
  strcat(supplies_list->battery_capacity_path,"/capacity");
}

static int init_supplies(struct charger *charger){
  int online_ac = 0,online_usb=0,ac,usb;
  charger->online=0;
  charger->charger_type=NONE;
  init_sysfs_paths();
  ac=read_file_int(supplies_list->ac_online_path,&online_ac);
  usb=read_file_int(supplies_list->usb_online_path,&online_usb);
  if(ac!=-1 || usb!=-1){
    //LOGI("ac => fd= %d,online=%d\tusb=>fd=%d,online=%d\n",ac,online_ac,usb,online_usb);
    if(online_ac && ac!=-1)
      charger->charger_type=AC;
    else if(online_usb && usb!=-1)
      charger->charger_type=USB;
  }
  else{
    LOGE("Error opening paths ac=%d usb=%d\n",ac,usb);
    return -1;
}
  charger->capacity= get_battery_capacity();
  charger->online=1;
  LOGI("Charger_online= %d (0=NONE,1=AC,2=USB)\n",charger->charger_type);
  charger->next_supply_check = curr_time_ms() + UNPLUGGED_SHUTDOWN_TIME;
  return 0;
}


static int update_supply(struct charger *charger){
  int supply=charger->charger_type;
  int online = 0,was_online=0;
  char *path;

  if(supply == AC)
    path=supplies_list->ac_online_path;
  else if(supply == USB)
    path=supplies_list->usb_online_path;

  if(read_file_int(path,&online)==-1){
    return -1;
  }

  if(charger->online!=online){
    LOGI("Charger online from %d to %d\t",charger->online,online);
    if(online==0)LOGI("Disconnected\n");
    else LOGI("Reconnected\n");
}
  charger->online = online;
  charger->capacity=get_battery_capacity();
  return 0;
}

static void check_status(struct charger *charger, int64_t now){
  if(now>=charger->next_supply_check){
    update_supply(charger);
    LOGI("Battery status %d %%\n",charger->capacity);

    if(charger->online){
      charger->next_supply_check = now + UNPLUGGED_SHUTDOWN_TIME;
      LOGI("Next power check in %d sec\n",(charger->next_supply_check)/MSEC_PER_SEC);
      return;
    }
    android_reboot(ANDROID_RB_POWEROFF, 0, 0);
  }
  return;
}

static int request_suspend(bool enable)
{
  int fd;
  if (enable){
    gr_fb_blank(true);
#ifdef CHARGER_ENABLE_SUSPEND
    return autosuspend_enable();
#elif defined DIM_SCREEN && defined BRIGHTNESS_PATH
    fd = open(BRIGHTNESS_PATH,O_WRONLY);
    write(fd,"20",strlen("20"));
    close(fd);
    return 0;
#endif
  }
  gr_fb_blank(false);
#ifdef CHARGER_ENABLE_SUSPEND
  return autosuspend_disable();
#elif defined DIM_SCREEN && defined BRIGHTNESS_PATH
  fd = open(BRIGHTNESS_PATH,O_WRONLY);
  write(fd,MAX_BRIGHTNESS,strlen(MAX_BRIGHTNESS));
  close(fd);
  return 0;
#endif
}

static int draw_text(const char *str, int x, int y)
{
  int str_len_px = gr_measure(str);

  if (x < 0)
    x = (gr_fb_width() - str_len_px) / 2;
  if (y < 0)
    y = (gr_fb_height() - char_height) / 2;
  gr_text(x, y, str, 0);

  return y + char_height;
}

static void android_green(void)
{
  gr_color(0xa4, 0xc6, 0x39, 255);
}

static void draw_capacity(struct charger *charger)
{
  char cap_str[64];
  int x, y;
  int str_len_px;

  snprintf(cap_str, sizeof(cap_str), "%d%%", charger->capacity);
  str_len_px = gr_measure(cap_str);
  x = (gr_fb_width() - str_len_px) / 2;
  y = (gr_fb_height() + char_height) / 2;
  android_green();
  gr_text(x, y, cap_str, 0);
}

/* returns the last y-offset of where the surface ends */
static int draw_surface_centered(struct charger *charger, gr_surface surface)
{
  int w;
  int h;
  int x;
  int y;

  w = gr_get_width(surface);
  h = gr_get_height(surface);
  x = (gr_fb_width() - w) / 2 ;
  y = (gr_fb_height() - h) / 2 ;

  LOGV("drawing surface %dx%d+%d+%d\n", w, h, x, y);
  gr_blit(surface, 0, 0, w, h, x, y);
  return y + h;
}

static void draw_unknown(struct charger *charger)
{
  int y;
  if (charger->surf_unknown) {
    draw_surface_centered(charger, charger->surf_unknown);
  } else {
    android_green();
    y = draw_text("Charging!", -1, -1);
    draw_text("?\?/100", -1, y + 25);
  }
}

static void draw_battery(struct charger *charger)
{
  struct animation *batt_anim = &battery_animation;
  struct frame *frame = &batt_anim->frames[batt_anim->cur_frame];

  if (batt_anim->num_frames != 0) {
    draw_surface_centered(charger, frame->surface);
    LOGV("drawing frame #%d name=%s min_cap=%d time=%d\n",
	 batt_anim->cur_frame, frame->name, frame->min_capacity,
	 frame->disp_time);
  }
}

static void redraw_screen(struct charger *charger)
{
  struct animation *batt_anim = charger->batt_anim;

  clear_screen();

  /* try to display *something* */
  if (batt_anim->capacity < 0 || batt_anim->num_frames == 0)
    draw_unknown(charger);
  else {
    draw_battery(charger);
    draw_capacity(charger);
  }
  gr_flip();
}

static void kick_animation(struct animation *anim)
{
  anim->run = true;
}

static void reset_animation(struct animation *anim)
{
  anim->cur_cycle = 0;
  anim->cur_frame = 0;
  anim->run = false;
}

static void update_screen_state(struct charger *charger,int64_t now){
  int cur_frame;
  int disp_time;
  struct animation *batt_anim=charger->batt_anim;
  if (!batt_anim->run || now < charger->next_screen_transition)
    return;

  /* animation is over, blank screen and leave */
  if (batt_anim->cur_cycle == batt_anim->num_cycles) {
    reset_animation(batt_anim);
    charger->next_screen_transition = -1;
    LOGV("[%lld] animation done\n", now);
    if (charger->online > 0)
      request_suspend(true);
    return;
  }

  disp_time = batt_anim->frames[batt_anim->cur_frame].disp_time;

  /* animation starting, set up the animation */
  if (batt_anim->cur_frame == 0) {
    int batt_cap;
    int ret;

    LOGV("[%lld] animation starting\n", now);
    batt_cap = get_battery_capacity(charger);
    if (batt_cap >= 0 && batt_anim->num_frames != 0) {
      int i;

      /* find first frame given current capacity */
      for (i = 1; i < batt_anim->num_frames; i++) {
	if (batt_cap < batt_anim->frames[i].min_capacity)
	  break;
      }
      batt_anim->cur_frame = i - 1;

      /* show the first frame for twice as long */
      disp_time = batt_anim->frames[batt_anim->cur_frame].disp_time * 2;
    }

    batt_anim->capacity = batt_cap;
  }

  /* unblank the screen  on first cycle */
  if (batt_anim->cur_cycle == 0)
    gr_fb_blank(false);

  /* draw the new frame (@ cur_frame) */
  redraw_screen(charger);

  /* if we don't have anim frames, we only have one image, so just bump
   * the cycle counter and exit
   */
  if (batt_anim->num_frames == 0 || batt_anim->capacity < 0) {
    LOGV("[%lld] animation missing or unknown battery status\n", now);
    charger->next_screen_transition = now + BATTERY_UNKNOWN_TIME;
    batt_anim->cur_cycle++;
    return;
  }

  /* schedule next screen transition */
  charger->next_screen_transition = now + disp_time;

  /* advance frame cntr to the next valid frame
   * if necessary, advance cycle cntr, and reset frame cntr
   */
  batt_anim->cur_frame++;

  /* if the frame is used for level-only, that is only show it when it's
   * the current level, skip it during the animation.
   */
  while (batt_anim->cur_frame < batt_anim->num_frames &&
	 batt_anim->frames[batt_anim->cur_frame].level_only)
    batt_anim->cur_frame++;
  if (batt_anim->cur_frame >= batt_anim->num_frames) {
    batt_anim->cur_cycle++;
    batt_anim->cur_frame = 0;

    /* don't reset the cycle counter, since we use that as a signal
     * in a test above to check if animation is over
     */
  }
}

static int set_key_callback(int code, int value, void *data)
{
  struct charger *charger = data;
  int64_t now = curr_time_ms();
  int down = !!value;

  if (code > KEY_MAX)
    return -1;

  /* ignore events that don't modify our state */
  if (charger->keys[code].down == down)
    return 0;

  /* only record the down even timestamp, as the amount
   * of time the key spent not being pressed is not useful */
  if (down)
    charger->keys[code].timestamp = now;
  charger->keys[code].down = down;
  charger->keys[code].pending = true;
  if (down) {
    LOGV("[%lld] key[%d] down\n", now, code);
  } else {
    int64_t duration = now - charger->keys[code].timestamp;
    int64_t secs = duration / 1000;
    int64_t msecs = duration - secs * 1000;
    LOGV("[%lld] key[%d] up (was down for %lld.%lldsec)\n", now,
	 code, secs, msecs);
  }

  return 0;
}

static void update_input_state(struct charger *charger,
                               struct input_event *ev)
{
  if (ev->type != EV_KEY)
    return;
  set_key_callback(ev->code, ev->value, charger);
}

static void set_next_key_check(struct charger *charger,
                               struct key_state *key,
                               int64_t timeout)
{
  int64_t then = key->timestamp + timeout;

  if (charger->next_key_check == -1 || then < charger->next_key_check)
    charger->next_key_check = then;
}

static void process_key(struct charger *charger, int code, int64_t now)
{
  struct key_state *key = &charger->keys[code];
  int64_t next_key_check;

  if (code == KEY_POWER) {
    if (key->down) {
      int64_t reboot_timeout = key->timestamp + POWER_ON_KEY_TIME;
      if (now >= reboot_timeout) {
	LOGI("[%lld] rebooting\n", now);
	android_reboot(ANDROID_RB_RESTART, 0, 0);
      } else {
	/* if the key is pressed but timeout hasn't expired,
	 * make sure we wake up at the right-ish time to check
	 */
	set_next_key_check(charger, key, POWER_ON_KEY_TIME);
      }
    } else {
      /* if the power key got released, force screen state cycle */
      if (key->pending) {
	request_suspend(false);
	kick_animation(charger->batt_anim);
      }
    }
  }

  key->pending = false;
}

static void handle_input_state(struct charger *charger, int64_t now)
{
  process_key(charger, KEY_POWER, now);

  if (charger->next_key_check != -1 && now > charger->next_key_check)
    charger->next_key_check = -1;
}

static void wait_next_event(struct charger *charger, int64_t now)
{
  int64_t next_event = INT64_MAX;
  int64_t timeout;
  struct input_event ev;
  int ret;

  LOGV("[%lld] next screen: %lld next key: %lld\n", now,
       charger->next_screen_transition, charger->next_key_check);

  if (charger->next_screen_transition != -1)
    next_event = charger->next_screen_transition;
  if (charger->next_key_check != -1 && charger->next_key_check < next_event)
    next_event = charger->next_key_check;
  if (next_event != -1 && next_event != INT64_MAX)
    timeout = max(0, next_event - now);
  else
    timeout = -1;
  LOGV("[%lld] blocking (%lld)\n", now, timeout);
  ret = ev_wait((int)timeout);
  if (!ret)
    ev_dispatch();
}

static int input_callback(int fd, short revents, void *data)
{
  struct charger *charger = data;
  struct input_event ev;
  int ret;

  ret = ev_get_input(fd, revents, &ev);
  if (ret)
    return -1;
  update_input_state(charger, &ev);
  return 0;
}

static void event_loop(struct charger *charger)
{
  int64_t now;
  int ret;

  while (true) {
    now=curr_time_ms();
    LOGV("[%lld] event_loop()\n", now);
    handle_input_state(charger, now);
    check_status(charger,now);
    /* do screen update last in case any of the above want to start
     * screen transitions (animations, etc)
     */
    update_screen_state(charger, now);
    wait_next_event(charger, now);
  }
}

int main(int argc, char **argv)
{
  int ret;
  struct charger *charger = &charger_state;

  int64_t now = curr_time_ms() - 1;
  int fd;
  int i;

  charger->batt_anim=&battery_animation;
  klog_init();
  klog_set_level(CHARGER_KLOG_LEVEL);

  dump_last_kmsg();

  LOGI("--------------- STARTING CHARGER MODE ---------------\n");

  gr_init();
  gr_font_size(&char_width, &char_height);

  ev_init(input_callback, charger);

  LOGI("Creating surface \n");
  ret = res_create_surface("charger/battery_fail", &charger->surf_unknown);
  if (ret < 0) {
    LOGE("Cannot load image\n");
    charger->surf_unknown = NULL;
  }

  for (i = 0; i < charger->batt_anim->num_frames; i++) {
    struct frame *frame = &charger->batt_anim->frames[i];

    ret = res_create_surface(frame->name, &frame->surface);
    if (ret < 0) {
      LOGE("Cannot load image %s\n", frame->name);
      /* TODO: free the already allocated surfaces... */
      charger->batt_anim->num_frames = 0;
      charger->batt_anim->num_cycles = 1;
      break;
    }
  }

  supplies_list=&supply;
  LOGI("Starting supply init \n");
  if(init_supplies(charger)==-1) return -1;
  LOGI("Starting key event callback \n");
  ev_sync_key_state(set_key_callback, charger);
  charger->next_screen_transition = now - 1;
  charger->next_key_check = -1;
  reset_animation(charger->batt_anim);
  kick_animation(charger->batt_anim);
  LOGI("Starting event loop\n");
  event_loop(charger);
  gr_exit();
  return 0;
}
