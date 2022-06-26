local w

-- APIs
local component = require("component")
local computer = require("computer")
local term = require("term")
local event = require("event")
local fs = require("filesystem")
local serialization = require("serialization")
local unicode = require("unicode")
local colors = {-- loosely based on CC colors
  green     = 0x008000,
  brown     = 0x804040,
  black     = 0x000000,
  pink      = 0xFF8000,
  yellow    = 0xFFFF00,
  orange    = 0xFFB040,
  purple    = 0x800080,
  magenta   = 0xFF80FF,
  red       = 0xFF0000,
  cyan      = 0x008080,
  white     = 0xFFFFFF,
  lightBlue = 0x8080FF,
  blue      = 0x0000FF,
  gray      = 0x404040,
  lightGray = 0xB0B0B0, -- silver
  lime      = 0x66FF66,
}

-- properties
local data = { }
local data_name = nil
local data_handlers = { }

local device_handlers = {}

local event_handlers = {}
local event_timers = {}

local monitors = {}
local monitor_textScale = 0.5

-- colors are cached to reduce GPU load on OC
local monitor_colorFront = colors.white
local monitor_colorBackground = colors.black

local page_handlers = {}
local page_endText = ""
local page_callbackDisplay
local page_callbackKey

local run_refreshPeriod_s = 20.0
local status_period_s = 1.0

local styles = {
  normal   = { front = colors.black    , back = colors.lightGray },
  good     = { front = colors.lime     , back = colors.lightGray },
  bad      = { front = colors.red      , back = colors.lightGray },
  disabled = { front = colors.gray     , back = colors.lightGray },
  help     = { front = colors.white    , back = colors.blue      },
  header   = { front = colors.orange   , back = colors.black     },
  control  = { front = colors.white    , back = colors.blue      },
  selected = { front = colors.black    , back = colors.lightBlue },
  warning  = { front = colors.white    , back = colors.red       },
  success  = { front = colors.white    , back = colors.lime      },
}

----------- Terminal & monitor support

local function setMonitorColorFrontBack(colorFront, colorBackground)
  if monitor_colorFront ~= colorFront then
    monitor_colorFront = colorFront
    component.gpu.setForeground(monitor_colorFront)
  end
  if monitor_colorBackground ~= colorBackground then
    monitor_colorBackground = colorBackground
    component.gpu.setBackground(monitor_colorBackground)
  end
end

local function write(text)
  if term.isAvailable() then
    local xSize, ySize = w.getResolution()
    if xSize then
      local x, y = w.getCursorPos()
      component.gpu.set(x, y, text)
      w.setCursorPos(x + #text, y)
    end
  end
end

local function getCursorPos()
  local x, y = term.getCursor()
  return x, y
end

local function setCursorPos(x, y)
  if term.isAvailable() then
    term.setCursor(x, y)
  end
end

local function getResolution()
  local sizeX, sizeY = component.gpu.getResolution()
  return sizeX, sizeY
end

local function setColorNormal()
  w.setMonitorColorFrontBack(styles.normal.front, styles.normal.back)
end

local function setColorGood()
  w.setMonitorColorFrontBack(styles.good.front, styles.good.back)
end

local function setColorBad()
  w.setMonitorColorFrontBack(styles.bad.front, styles.bad.back)
end

local function setColorDisabled()
  w.setMonitorColorFrontBack(styles.disabled.front, styles.disabled.back)
end

local function setColorHelp()
  w.setMonitorColorFrontBack(styles.help.front, styles.help.back)
end

local function setColorHeader()
  w.setMonitorColorFrontBack(styles.header.front, styles.header.back)
end

local function setColorControl()
  w.setMonitorColorFrontBack(styles.control.front, styles.control.back)
end

local function setColorSelected()
  w.setMonitorColorFrontBack(styles.selected.front, styles.selected.back)
end

local function setColorWarning()
  w.setMonitorColorFrontBack(styles.warning.front, styles.warning.back)
end

local function setColorSuccess()
  w.setMonitorColorFrontBack(styles.success.front, styles.success.back)
end

local function clear(colorFront, colorBack)
  if colorFront == nil or colorBack == nil then
    w.setColorNormal()
  else
    w.setMonitorColorFrontBack(colorFront, colorBack)
  end
  term.clear()
  w.setCursorPos(1, 1)
end

local function clearLine()
  term.clearLine()
  local x, y = w.getCursorPos()
  w.setCursorPos(1, y)
end

local function writeLn(text)
  if term.isAvailable() then
    w.write(text)
    local x, y = w.getCursorPos()
    local xSize, ySize = w.getResolution()
    if y > ySize - 1 then
      y = 1
    end
    w.setCursorPos(1, y + 1)
  end
end

local function writeMultiLine(text)
  local textToParse = text or ""
  for line in string.gmatch(textToParse, "[^\n]+") do
    if line ~= "" then
      w.writeLn(line)
    end
  end
end

local function writeCentered(y, text)
  local unused
  if text == nil then
    text = y
    unused, y = w.getCursorPos()
  end
  
  if term.isAvailable() then
    local xSize, ySize = w.getResolution()
    if xSize ~= nil then
      component.gpu.set((xSize - text:len()) / 2, y, text)
    end
    w.setCursorPos(1, y + 1)
  end
end

local function writeFullLine(text)
  if term.isAvailable() then
    w.write(text)
    local xSize, ySize = w.getResolution()
    local xCursor, yCursor = w.getCursorPos()
    for i = xCursor, xSize do
      w.write(" ")
    end
    w.setCursorPos(1, yCursor + 1)
  end
end

----------- Page support

local function page_begin(text)
  w.clear()
  w.setCursorPos(1, 1)
  w.setColorHeader()
  w.clearLine()
  w.writeCentered(1, text)
  w.status_refresh()
  w.setCursorPos(1, 2)
  w.setColorNormal()
end

local function page_colors()
  w.clear(colors.white, colors.black)
  for key, value in pairs(colors) do
    local text = string.format("%12s", key)
    w.setMonitorColorFrontBack(colors.white, colors.black)
    w.write(text .. " ")
    w.setMonitorColorFrontBack(value, colors.black)
    w.write(" " .. text .. " ")
    w.setMonitorColorFrontBack(colors.black, value)
    w.write(" " .. text .. " ")
    w.setMonitorColorFrontBack(colors.white, value)
    w.write(" " .. text .. " ")
    w.setMonitorColorFrontBack(value, colors.white)
    w.write(" " .. text .. " ")
    w.writeLn("")
  end
  w.writeLn("")
  local index = 0
  for key, value in pairs(styles) do
    local text = string.format("%12s", key)
    if index % 2 == 0 then
      w.setMonitorColorFrontBack(colors.white, colors.black)
      w.write(text .. " ")
      w.setMonitorColorFrontBack(value.front, value.back)
      w.write(" " .. text .. " ")
    else
      w.setMonitorColorFrontBack(value.front, value.back)
      w.write(" " .. text .. " ")
      w.setMonitorColorFrontBack(colors.white, colors.black)
      w.write(text .. " ")
      w.writeLn("")
    end
    index = index + 1
  end
  w.setMonitorColorFrontBack(colors.white, colors.black)
end

local function page_end()
  w.setCursorPos(1, 23)
  w.setColorControl()
  w.writeFullLine(page_endText)
end

local function page_getCallbackDisplay()
  return page_callbackDisplay
end

local function page_register(index, callbackDisplay, callbackKey)
  page_handlers[index] = { display = callbackDisplay, key = callbackKey }
end

local function page_setEndText(text)
  page_endText = text
end

----------- Status line support

local status_clockTarget = -1 -- < 0 when stopped, < clock when elapsed, > clock when ticking
local status_isWarning = false
local status_line = 0
local status_text = ""
local function status_clear()
  if status_clockTarget > 0 then
    status_clockTarget = -1
    w.event_timer_stop("status")
    local xSize, ySize = w.getResolution()
    w.setCursorPos(1, ySize)
    w.setColorNormal()
    w.clearLine()
  end
end
local function status_isActive()
  return status_clockTarget > 0 and w.event_clock() < status_clockTarget
end
local function status_show(isWarning, text)
  if isWarning or not w.status_isActive() then
    status_line = 1
    status_isWarning = isWarning
    status_text = {}
    local textToParse = (text and text ~= "") and text or "???"
    for line in string.gmatch(textToParse, "[^\n]+") do
      if line ~= "" then
        table.insert(status_text, line)
      end
    end
    if isWarning then
      status_clockTarget = w.event_clock() + 1.0 * #status_text
    else
      status_clockTarget = w.event_clock() + 0.5 * #status_text
    end
    w.event_timer_start("status", status_period_s, "timer_status")
  end
  -- always refresh as a visual clue
  w.status_refresh()
end
local function status_refresh()
  if status_clockTarget > 0 then
    if w.event_clock() > status_clockTarget and status_line == 1 then
      w.status_clear()
    else
      local xSize, ySize = w.getResolution()
      w.setCursorPos(1, ySize)
      w.setColorNormal()
      w.clearLine()

      if status_isWarning then
        w.setColorWarning()
      else
        w.setColorSuccess()
      end
      local text = status_text[status_line]
      w.writeCentered(" " .. text .. " ")
      w.setColorNormal()
    end
  end
end
local function status_showWarning(text)
  w.status_show(true, text)
end
local function status_showSuccess(text)
  w.status_show(false, text)
end
local function status_tick()
  if status_clockTarget > -1 then
    local clockCurrent = w.event_clock()
    if clockCurrent > status_clockTarget then
      w.status_clear()
    else
      status_line = (status_line % #status_text) + 1
      w.status_refresh()
    end
  end
end

----------- Formatting

local function format_float(value, nbchar)
  local str = "?"
  if value ~= nil then
    if type(value) == "number" then
      str = string.format("%g", value)
    else
      str = type(value)
    end
  end
  if nbchar ~= nil then
    str = string.sub("               " .. str, -nbchar)
  end
  return str
end

local function format_integer(value, nbchar)
  local str = "?"
  if value ~= nil then
    if type(value) == "number" then
      str = string.format("%d", math.floor(value))
    else
      str = type(value)
    end
  end
  if nbchar ~= nil then
    str = string.sub("               " .. str, -nbchar)
  end
  return str
end

local function format_boolean(value, strTrue, strFalse)
  if value ~= nil then
    if type(value) == "boolean" then
      if value then
        return strTrue
      else
        return strFalse
      end
    else
      return type(value)
    end
  end
  return "?"
end

local function format_string(value, nbchar)
  local str = "?"
  if value ~= nil then
    str = "" .. value
  end
  if nbchar ~= nil then
    if #str > math.abs(nbchar) then
      str = string.sub(str, 1, math.abs(nbchar) - 1) .. "~"
    else
      str = string.sub(str .. "                                                  ", 1, nbchar)
    end
  end
  return str
end

local function format_address(value)
  local str = "?"
  if value ~= nil then
    str = "" .. value
  end
  str = string.sub(str, 10, 100)
  return str
end

local function format_charNumber(value)
  if  value ~= nil
  and type(value) == "number"
  and value <= 65535
  and value >= 0 then
    return unicode.char(value)
  else
    return string.char(0)
  end
end

----------- Input controls

local function input_readInteger(currentValue)
  local inputAbort = false
  local input = w.format_integer(currentValue)
  if input == "0" then
    input = ""
  end
  local ignoreNextChar = false
  local x, y = w.getCursorPos()
  
  term.setCursorBlink(true)
  repeat
    w.setCursorPos(x, y)
    w.setColorNormal()
    w.write(input .. "            ")
    input = string.sub(input, -9)
    w.setCursorPos(x + #input, y)
    
    local params = { event.pull() }
    local eventName = params[1]
    local address = params[2]
    if address == nil then address = "none" end
    local firstParam = params[3]
    if firstParam == nil then firstParam = "none" end
    if eventName == "key_down" then
      local character = format_charNumber(params[3])
      local keycode = params[4]
      
      if keycode >= 2 and keycode <= 10 then -- 1 to 9
        input = input .. w.format_integer(keycode - 1)
        ignoreNextChar = true
      elseif keycode == 11 or keycode == 82 then -- 0 & keypad 0
        input = input .. "0"
        ignoreNextChar = true
      elseif keycode >= 79 and keycode <= 81 then -- keypad 1 to 3
        input = input .. w.format_integer(keycode - 78)
        ignoreNextChar = true
      elseif keycode >= 75 and keycode <= 77 then -- keypad 4 to 6
        input = input .. w.format_integer(keycode - 71)
        ignoreNextChar = true
      elseif keycode >= 71 and keycode <= 73 then -- keypad 7 to 9
        input = input .. w.format_integer(keycode - 64)
        ignoreNextChar = true
      elseif keycode == 14 then -- Backspace
        input = string.sub(input, 1, string.len(input) - 1)
        ignoreNextChar = true
      elseif keycode == 211 then -- Delete
        input = ""
        ignoreNextChar = true
      elseif keycode == 28 then -- Enter
        inputAbort = true
        ignoreNextChar = true
      elseif keycode == 74 or keycode == 12 or keycode == 49 then -- - on numeric keypad or - on US top or n letter
        if string.sub(input, 1, 1) == "-" then
          input = string.sub(input, 2)
        else
          input = "-" .. input
        end
        ignoreNextChar = true
      elseif keycode == 78 then -- +
        if string.sub(input, 1, 1) == "-" then
          input = string.sub(input, 2)
        end
        ignoreNextChar = true
      else
        ignoreNextChar = false
        -- w.status_showWarning("Key " .. keycode .. " is not supported here")
      end
      
      if ignoreNextChar then
        ignoreNextChar = false
        -- w.status_showWarning("Ignored char #" .. string.byte(character) .. " '" .. character .. "'")
      elseif character >= '0' and character <= '9' then -- 0 to 9
        input = input .. character
      elseif character == '-' or character == 'n' or character == 'N' then -- - or N
        if string.sub(input, 1, 1) == "-" then
          input = string.sub(input, 2)
        else
          input = "-" .. input
        end
      elseif character == '+' or character == 'p' or character == 'P' then -- + or P
        if string.sub(input, 1, 1) == "-" then
          input = string.sub(input, 2)
        end
      else
        w.status_showWarning("Key '" .. character .. "' is not supported here (" .. string.byte(character) .. ")")
      end
      
    elseif eventName == "interrupted" then
      inputAbort = true
      
    else
      local isSupported, needRedraw = w.event_handler(eventName, firstParam)
      if not isSupported then
        w.status_showWarning("Event '" .. eventName .. "', " .. address .. " , " .. firstParam .. " is unsupported")
      end
    end
  until inputAbort
  term.setCursorBlink(false)
  w.setCursorPos(1, y + 1)
  if input == "" or input == "-" then
    return currentValue
  else
    return tonumber(input)
  end
end

local function input_readText(currentValue)
  local inputAbort = false
  local input = w.format_string(currentValue)
  local ignoreNextChar = false
  local x, y = w.getCursorPos()
  
  term.setCursorBlink(true)
  repeat
    -- update display clearing extra characters
    w.setCursorPos(x, y)
    w.setColorNormal()
    w.write(w.format_string(input, 37))
    -- truncate input and set caret position
    input = string.sub(input, -36)
    w.setCursorPos(x + #input, y)
    
    local params = { event.pull() }
    local eventName = params[1]
    local address = params[2]
    if address == nil then address = "none" end
    local firstParam = params[3]
    if firstParam == nil then firstParam = "none" end
    if eventName == "key_down" then
      local character = format_charNumber(params[3])
      local keycode = params[4]
      
      if keycode == 14 then -- Backspace
        input = string.sub(input, 1, string.len(input) - 1)
        ignoreNextChar = true
      elseif keycode == 211 then -- Delete
        input = ""
        ignoreNextChar = true
      elseif keycode == 28 then -- Enter
        inputAbort = true
        ignoreNextChar = true
      else
        ignoreNextChar = false
        -- w.status_showWarning("Key " .. keycode .. " is not supported here")
      end
      
      if ignoreNextChar then
        ignoreNextChar = false
        -- w.status_showWarning("Ignored char #" .. string.byte(character) .. " '" .. character .. "'")
      elseif character >= ' ' and character <= '~' then -- any ASCII table minus controls and DEL
        input = input .. character
      else
        w.status_showWarning("Key '" .. character .. "' is not supported here (" .. string.byte(character) .. ")")
      end
      
    elseif eventName == "interrupted" then
      inputAbort = true
      
    else
      local isSupported, needRedraw = w.event_handler(eventName, firstParam)
      if not isSupported then
        w.status_showWarning("Event '" .. eventName .. "', " .. address .. ", " .. firstParam .. " is unsupported")
      end
    end
  until inputAbort
  term.setCursorBlink(false)
  w.setCursorPos(1, y + 1)
  if input == "" then
    return currentValue
  else
    return input
  end
end

local function input_readConfirmation(message)
  if message == nil then
    message = "Are you sure? (Y/n)"
  end
  w.status_showWarning(message)
  repeat
    local params = { event.pull() }
    local eventName = params[1]
    local address = params[2]
    if address == nil then address = "none" end
    local firstParam = params[3]
    if firstParam == nil then firstParam = "none" end
    if eventName == "key_down" then
      local character = format_charNumber(params[3])
      local keycode = params[4]
      
      if keycode == 28 then -- Return or Enter
        w.status_clear()
        return true
      end
      
      w.status_clear()
      if character == 'y' or character == 'Y' then -- Y
        return true
      else
        return false
      end
      
    elseif eventName == "interrupted" then
      return false
      
    else
      local isSupported, needRedraw = w.event_handler(eventName, firstParam)
      if not isSupported then
        w.status_showWarning("Event '" .. eventName .. "', " .. firstParam .. " is unsupported")
      end
    end
    if not w.status_isActive() then
      w.status_showWarning(message)
    end
  until false
end

local function input_readEnum(currentValue, list, toValue, toDescription, noValue)
  local inputAbort = false
  local inputKey = nil
  local input = nil
  local inputDescription = nil
  local ignoreNextChar = false
  local x, y = w.getCursorPos()
  
  w.setCursorPos(1, 17)
  for key, entry in pairs(list) do
    if toValue(entry) == currentValue then
      inputKey = key
    end
  end
  
  term.setCursorBlink(true)
  repeat
    w.setCursorPos(x, y)
    w.setColorNormal()
    if #list == 0 then
      inputKey = nil
    end
    if inputKey == nil then
      if currentValue ~= nil then
        input = noValue
        inputDescription = "Press enter to return previous entry"
      else
        input = noValue
        inputDescription = "Press enter to close listing"
      end
    else
      if inputKey < 1 then
        inputKey = #list
      elseif inputKey > #list then
        inputKey = 1
      end
      
      input = toValue(list[inputKey])
      inputDescription = toDescription(list[inputKey])
    end
    w.setColorNormal()
    w.write(input .. "                                                  ")
    w.setCursorPos(1, y + 1)
    w.setColorDisabled()
    w.write(inputDescription .. "                                                  ")
    
    local params = { event.pull() }
    local eventName = params[1]
    local address = params[2]
    if address == nil then address = "none" end
    local firstParam = params[3]
    if firstParam == nil then firstParam = "none" end
    if eventName == "key_down" then
      local character = format_charNumber(params[3])
      local keycode = params[4]
      
      if keycode == 14 or keycode == 211 then -- Backspace or Delete
        inputKey = nil
        ignoreNextChar = true
      elseif keycode == 200 or keycode == 203 or keycode == 78 then -- Up or Left or +
        if inputKey == nil then
          inputKey = 1
        else
          inputKey = inputKey - 1
        end
        ignoreNextChar = true
      elseif keycode == 208 or keycode == 205 or keycode == 74 then -- Down or Right or -
        if inputKey == nil then
          inputKey = 1
        else
          inputKey = inputKey + 1
        end
        ignoreNextChar = true
      elseif keycode == 28 then -- Enter
        inputAbort = true
        ignoreNextChar = true
      else
        ignoreNextChar = false
        -- w.status_showWarning("Key " .. keycode .. " is not supported here")
      end
      
      if ignoreNextChar then
        ignoreNextChar = false
        -- w.status_showWarning("Ignored char #" .. string.byte(character) .. " '" .. character .. "'")
      elseif character == '+' then -- +
        if inputKey == nil then
          inputKey = 1
        else
          inputKey = inputKey - 1
        end
      elseif character == '-' then -- -
        if inputKey == nil then
          inputKey = 1
        else
          inputKey = inputKey + 1
        end
      else
        w.status_showWarning("Key '" .. character .. "' is not supported here (" .. string.byte(character) .. ")")
      end
      
    elseif eventName == "interrupted" then
      inputAbort = true
      
    elseif not w.event_handler(eventName, firstParam) then
      w.status_showWarning("Event '" .. eventName .. "', " .. address .. ", " .. firstParam .. " is unsupported")
    end
  until inputAbort
  term.setCursorBlink(false)
  w.setCursorPos(1, y + 1)
  w.clearLine()
  if inputKey == nil then
    return nil
  else
    return toValue(list[inputKey])
  end
end

----------- Event handlers

local function reboot()
  computer.shutdown(true)
end

local function sleep(delay)
  os.sleep(delay)
end

-- return a global clock measured in second
local function event_clock()
  return computer.uptime()
end

local function event_timer_start(name_, period_s_, eventId_)
  local name = name_ or "-nameless-"
  local eventId = eventId_ or "timer_" .. name
  -- check for an already active timer
  local countActives = 0
  for id, entry in pairs(event_timers) do
    if entry.name == name and entry.active then -- already one started
      countActives = countActives + 1
    end
  end
  if countActives > 0 then
    if name ~= "status" then -- don't report status timer overlaps to prevent a stack overflow
      w.status_showWarning("Timer already started for " .. name)
    end
    return
  end
  -- start a new timer
  local period_s = period_s_ or 1.0
  local id = event.timer(period_s, function () w.event_timer_tick(name) end, math.huge)
  event_timers[id] = {
    active = true,
    eventId = eventId,
    name = name,
    period_s = period_s
  }
end

local function event_timer_stop(name_)
  local name = name_ or "-nameless-"
  for id, entry in pairs(event_timers) do
    if entry.name == name then
      if entry.active then -- kill any active one
        entry.active = false
        event.cancel(id)
      else -- purge all legacy ones
        event_timers[id] = nil
      end
    end
  end
end

local function event_timer_stopAll()
  for id, entry in pairs(event_timers) do
    if entry.active then
      event_timers[id] = nil
      event.cancel(id)
    end
  end
end

local function event_timer_tick(name)
  local entry = nil
  local isUnknown = true
  for idLoop, entryLoop in pairs(event_timers) do
    if entryLoop.name == name then
      isUnknown = false
      if entryLoop.active then
        entry = entryLoop
      end
    end
  end
  if isUnknown then -- unknown id, report a warning
    w.status_showWarning("Timer " .. name .. " is unknown")
    return
  end
  if entry == nil then -- dying timer, just ignore it
      return
  end
  if not entry.active then -- dying timer, just ignore it
      return
  end
  -- resolve the timer
  event.push(entry.eventId)
end

local function event_register(eventName, callback)
  event_handlers[eventName] = callback
end

-- returns isSupported, needRedraw
local function event_handler(eventName, param)
  local needRedraw = false
  if event_handlers[eventName] ~= nil then
    needRedraw = event_handlers[eventName](eventName, param)
  elseif eventName == "redstone" then
    -- w.event_redstone(param)
  elseif eventName == "key_up" then
  elseif eventName == "touch" then
    w.status_showSuccess("Use the keyboard, Luke!")
  elseif eventName == "drop" then
  elseif eventName == "drag" then
  elseif eventName == "scroll" then
  elseif eventName == "walk" then
  elseif eventName == "component_added" then
  elseif eventName == "component_removed" then
  elseif eventName == "component_available" then
  elseif eventName == "component_unavailable" then
  elseif eventName == "gpu_bound" then-- OpenOS internal event?
  elseif eventName == "term_available" then
    needRedraw = true
  elseif eventName == "term_unavailable" then
    needRedraw = true
  -- not supported: task_complete, rednet_message, modem_message
  else
    return false, needRedraw
  end
  return true, needRedraw
end

----------- Configuration

local function data_get()
  return data
end

local function data_inspect(key, value)
  local stringValue = type(value) .. ","
  if type(value) == "boolean" then
    if value then
      stringValue = "true,"
    else
      stringValue = "false,"
    end
  elseif type(value) == "number" then
    stringValue = value .. ","
  elseif type(value) == "string" then
    stringValue = "'" .. value .. "',"
  elseif type(value) == "table" then
    stringValue = "{"
  end
  print(" " .. key .. " = " .. stringValue)
  if type(value) == "table" then
    for subkey, subvalue in pairs(value) do
      w.data_inspect(subkey, subvalue)
    end
    print("}")
  end
end

local function data_read()
  w.data_shouldUpdateName()
  
  data = { }
  if fs.exists("/etc/shipdata.txt") then
    local size = fs.size("/etc/shipdata.txt")
    if size > 0 then
      local file = io.open("/etc/shipdata.txt", "r")
      if file ~= nil then
        local rawData = file:read("*all")
        if rawData ~= nil then
          data = serialization.unserialize(rawData)
        end
        file:close()
        if data == nil then
          data = {}
        end
      end
    end
  end
  
  for name, handlers in pairs(data_handlers) do
    handlers.read(data)
  end
end

local function data_save()
  for name, handlers in pairs(data_handlers) do
    handlers.save(data)
  end
  
  local file = io.open("/etc/shipdata.txt", "w")
  if file ~= nil then
    file:write(serialization.serialize(data))
    file:close()
  else
    w.status_showWarning("No file system")
    w.sleep(3.0)
  end
end

local function data_getName()
  if data_name ~= nil then
    return data_name
  else
    return "-noname-"
  end
end

local function data_setName()
  -- check if any named component is connected
  local componentName = "computer"
  for name, handlers in pairs(data_handlers) do
    if handlers.name ~= nil then
      componentName = name
    end
  end
  
  -- ask for a new name
  w.page_begin("<==== Set " .. componentName .. " name ====>")
  w.setCursorPos(1, 4)
  w.setColorHelp()
  w.writeFullLine(" Press enter to validate.")
  w.setCursorPos(1, 3)
  w.setColorNormal()
  w.write("Enter " .. componentName .. " name: ")
  data_name = w.input_readText(data_name)
  
  -- OpenComputers only allows to label filesystems => out
  
  -- update connected components
  for name, handlers in pairs(data_handlers) do
    if handlers.name ~= nil then
      handlers.name(data_name)
    end
  end
  
  -- w.reboot() -- not needed
end

local function data_shouldUpdateName()
  local shouldUpdateName = false
  
  -- check computer name
  data_name = "" .. computer.address()
  local nameDefault = data_name
  
  -- check connected components names
  for name, handlers in pairs(data_handlers) do
    if handlers.name ~= nil then
      local componentName = handlers.name()
      if componentName == "" then
        shouldUpdateName = true
      elseif shouldUpdateName then
        data_name = componentName
      elseif data_name ~= componentName then
        shouldUpdateName = data_name ~= nameDefault
        data_name = componentName
      end
    end
  end
  
  return shouldUpdateName
end

local function data_splitString(source, sep_)
  local sep = sep_ or ":"
  local fields = {}
  local pattern = string.format("([^%s]+)", sep)
  source:gsub(pattern, function(c) fields[#fields + 1] = c end)
  return fields
end

local function data_register(name, callbackRead, callbackSave, callbackName)
  -- read/save callbacks are always defined
  if callbackRead == nil then
    callbackRead = function() end
  end
  if callbackSave == nil then
    callbackSave = function() end
  end
  
  -- name callback is nil when not defined
  
  data_handlers[name] = { read = callbackRead, save = callbackSave, name = callbackName }
end

----------- Devices

local function device_get(address)
  return component.proxy(address)
end

local function device_getMonitors()
  return monitors
end

local function device_register(deviceType, callbackRegister, callbackUnregister)
  device_handlers[deviceType] = { register = callbackRegister, unregister = callbackUnregister }
end

----------- Main loop


local function boot()
  if not term.isAvailable() then
    computer.beep()
    os.exit()
  end
  if component.gpu.getDepth() < 4 then
    print("A tier 2 or higher GPU is required")
    print("A tier 2 or higher screen is required")
    os.exit()
  end
  print("loading...")
  
  math.randomseed(os.time())
  
  -- read configuration
  w.data_read()
  w.clear()
  print("data_read...")
  
  -- initial scanning
  monitors = {}
  w.page_begin(data_name .. " - Connecting...")
  w.writeLn("")
  
  for address, deviceType in component.list() do
    w.sleep(0)
    w.write("Checking " .. address .. " ")
    w.write(deviceType .. " ")
    local handlers = device_handlers[deviceType]
    if handlers ~= nil then
      w.write("wrapping!")
      handlers.register(deviceType, address, w.device_get(address))
    end
    
    w.writeLn("")
  end
  
  -- synchronize computer and connected components names
  local shouldUpdateName = w.data_shouldUpdateName()
  if shouldUpdateName then
    w.data_setName()
  end
  
  -- peripheral boot up
  if page_handlers['0'] == nil then
    w.status_showWarning("Missing handler for connection page '0'!")
    os.exit()
  end
  page_handlers['0'].display(true)
end

local function run()
  local abort = false
  local refresh = true
  local ignoreNextChar = false
  
  local function selectPage(index)
    if page_handlers[index] ~= nil then
      page_callbackDisplay = page_handlers[index].display
      page_callbackKey = page_handlers[index].key
      refresh = true
      return true
    end
    return false
  end
  
  -- start refresh timer
  w.event_register("timer_refresh", function() return page_callbackDisplay ~= page_handlers['0'].display end )
  w.event_register("timer_status" , function() w.status_tick() return false end )
  w.event_timer_start("refresh", run_refreshPeriod_s, "timer_refresh")
  
  -- register common events
  w.event_register("enabled" , function(eventName, param) w.status_showWarning("Enabled '" .. w.format_string(param) .. "'") end )
  w.event_register("disabled", function(eventName, param) w.status_showWarning("Disabled '" .. w.format_string(param) .. "'") end )
  
  -- main loop
  selectPage('0')
  repeat
    if refresh then
      w.clear()
      page_callbackDisplay(false)
      w.page_end()
      refresh = false
    end
    local params = { event.pull() }
    local eventName = params[1]
    local address = params[2]
    if address == nil then address = "none" end
    local firstParam = params[3]
    if firstParam == nil then firstParam = "none" end
    -- w.writeLn("...")
    -- w.writeLn("Event '" .. eventName .. "', " .. firstParam .. " received")
    -- w.sleep(0.2)
    
    if eventName == "key_down" then
      local character = format_charNumber(params[3])
      local keycode = params[4]
      
      ignoreNextChar = false
      if keycode == 11 or keycode == 82 then -- 0
        if selectPage('0') then
          ignoreNextChar = true
        end
      elseif keycode == 2 or keycode == 79 then -- 1
        if selectPage('1') then
          ignoreNextChar = true
        end
      elseif keycode == 3 or keycode == 80 then -- 2
        if selectPage('2') then
          ignoreNextChar = true
        end
      elseif keycode == 4 or keycode == 81 then -- 3
        if selectPage('3') then
          ignoreNextChar = true
        end
      elseif keycode == 5 or keycode == 82 then -- 4
        if selectPage('4') then
          ignoreNextChar = true
        end
      elseif keycode == 6 or keycode == 83 then -- 5
        if selectPage('5') then
          ignoreNextChar = true
        end
      else
        ignoreNextChar = false
        -- w.status_showWarning("Key " .. keycode .. " is not supported here")
      end
      
      if ignoreNextChar then
        ignoreNextChar = false
        -- w.status_showWarning("Ignored char #" .. string.byte(character) .. " '" .. character .. "'")
--      elseif character == 'x' or character == 'X' then -- x for eXit
--        -- event.pull() -- remove key_up event
--        abort = true
      elseif character == '0' then
        selectPage('0')
      elseif character == '1' then
        selectPage('1')
      elseif character == '2' then
        selectPage('2')
      elseif character == '3' then
        selectPage('3')
      elseif character == '4' then
        selectPage('4')
      elseif character == '5' then
        selectPage('5')
      elseif page_callbackKey ~= nil and page_callbackKey(character, keycode) then
        refresh = true
      elseif string.byte(character) ~= 0 then -- not a control char
        w.status_showWarning("Key '" .. character .. "' is not supported here (" .. string.byte(character) .. ")")
      end
      
    elseif eventName == "interrupted" then
      abort = true
      
    else
      local isSupported, needRedraw = w.event_handler(eventName, firstParam)
      if not isSupported then
        w.status_showWarning("Event '" .. eventName .. "', " .. firstParam .. " is unsupported")
      end
      refresh = needRedraw
    end
  until abort
  
  -- stop refresh timer
  w.event_timer_stop("refresh")
  w.event_timer_stopAll()
end

local function close()
  w.clear(colors.white, colors.black)
  for key, handlers in pairs(device_handlers) do
    w.writeLn("Closing " .. key)
    if handlers.unregister ~= nil then
      handlers.unregister(key)
    end
  end
  
  w.clear(colors.white, colors.black)
  w.setCursorPos(1, 1)
  w.writeLn("Program closed")
  w.writeLn("Type reboot to return to home page")
end

w = {
  setMonitorColorFrontBack = setMonitorColorFrontBack,
  write = write,
  getCursorPos = getCursorPos,
  setCursorPos = setCursorPos,
  getResolution = getResolution,
  setColorNormal = setColorNormal,
  setColorGood = setColorGood,
  setColorBad = setColorBad,
  setColorDisabled = setColorDisabled,
  setColorHelp = setColorHelp,
  setColorHeader = setColorHeader,
  setColorControl = setColorControl,
  setColorSelected = setColorSelected,
  setColorWarning = setColorWarning,
  setColorSuccess = setColorSuccess,
  clear = clear,
  clearLine = clearLine,
  writeLn = writeLn,
  writeMultiLine = writeMultiLine,
  writeCentered = writeCentered,
  writeFullLine = writeFullLine,
  page_begin = page_begin,
  page_colors = page_colors,
  page_end = page_end,
  page_getCallbackDisplay = page_getCallbackDisplay,
  page_register = page_register,
  page_setEndText = page_setEndText,
  status_clear = status_clear,
  status_isActive = status_isActive,
  status_show = status_show,
  status_refresh = status_refresh,
  status_showWarning = status_showWarning,
  status_showSuccess = status_showSuccess,
  status_tick = status_tick,
  format_float = format_float,
  format_integer = format_integer,
  format_boolean = format_boolean,
  format_string = format_string,
  format_address = format_address,
  format_charNumber = format_charNumber,
  input_readInteger = input_readInteger,
  input_readText = input_readText,
  input_readConfirmation = input_readConfirmation,
  input_readEnum = input_readEnum,
  reboot = reboot,
  sleep = sleep,
  event_clock = event_clock,
  event_timer_start = event_timer_start,
  event_timer_stop = event_timer_stop,
  event_timer_stopAll = event_timer_stopAll,
  event_timer_tick = event_timer_tick,
  event_register = event_register,
  event_handler = event_handler,
  data_get = data_get,
  data_inspect = data_inspect,
  data_read = data_read,
  data_save = data_save,
  data_getName = data_getName,
  data_setName = data_setName,
  data_shouldUpdateName = data_shouldUpdateName,
  data_splitString = data_splitString,
  data_register = data_register,
  device_get = device_get,
  device_getMonitors = device_getMonitors,
  device_register = device_register,
  boot = boot,
  run = run,
  close = close,
}

return w