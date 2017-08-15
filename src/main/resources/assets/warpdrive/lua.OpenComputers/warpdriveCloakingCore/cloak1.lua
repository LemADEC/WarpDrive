local component = require("component")
local term = require("term")

if not component.isAvailable("warpdriveCloakingCore") then
  print("No cloaking core detected")
else
  local cloakingCore = component.warpdriveCloakingCore
  cloakingCore.tier(1)
  cloakingCore.enable(true)
  local isValid, message = cloakingCore.isAssemblyValid()
  if isValid then
    print("Tier 1 cloaking is enabled")
  else
    print(message)
    print()
    print("In each of the 6 directions, you need to place exactly 2 Cloaking coils, for a total of 12 coils.")
    print("The 6 inner coils shall be exactly one block away from the core.")
    print("The cloaking field will extend 5 blocks past the outer 6 coils.")
    print("Power consumption scales with the amount of cloaked blocks.")
  end
end

print("")